package com.digitalasset.userservice.service;

import com.digitalasset.common.dto.UserDto;
import com.digitalasset.common.enums.KycStatus;
import com.digitalasset.common.enums.UserRole;
import com.digitalasset.common.events.KycVerifiedEvent;
import com.digitalasset.common.exceptions.BusinessException;
import com.digitalasset.common.security.JwtTokenProvider;
import com.digitalasset.userservice.dto.*;
import com.digitalasset.userservice.entity.User;
import com.digitalasset.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw BusinessException.conflict("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.conflict("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .country(request.getCountry())
                .role(UserRole.INVESTOR)
                .kycStatus(KycStatus.PENDING)
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new user: {} (id={})", saved.getUsername(), saved.getId());
        return toDto(saved);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> BusinessException.badRequest("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BusinessException.badRequest("Invalid credentials");
        }
        if (!user.isActive()) {
            throw BusinessException.forbidden("Account is deactivated");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        log.info("User logged in: {}", user.getUsername());

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(86400)
                .user(toDto(user))
                .build();
    }

    @Transactional(readOnly = true)
    public UserDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User", userId));
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("User", username));
        return toDto(user);
    }

    @Transactional
    public UserDto updateKycStatus(Long userId, KycUpdateRequest request, String updatedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User", userId));

        user.setKycStatus(request.getKycStatus());
        if (request.getDocumentRef() != null) {
            user.setKycDocumentRef(request.getDocumentRef());
        }

        User saved = userRepository.save(user);

        KycVerifiedEvent event = KycVerifiedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(saved.getId())
                .username(saved.getUsername())
                .kycStatus(saved.getKycStatus())
                .verifiedBy(updatedBy)
                .notes(request.getNotes())
                .build();

        kafkaTemplate.send(KycVerifiedEvent.TOPIC, event.getEventId(), event);
        log.info("KYC status updated for user {} to {}", userId, request.getKycStatus());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getPendingKycUsers() {
        return userRepository.findByKycStatus(KycStatus.PENDING)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .kycStatus(user.getKycStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
