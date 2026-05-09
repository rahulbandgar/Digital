package com.digitalasset.walletservice.service;

import com.digitalasset.common.dto.WalletDto;
import com.digitalasset.common.enums.AssetType;
import com.digitalasset.common.events.TokenTransferredEvent;
import com.digitalasset.common.exceptions.BusinessException;
import com.digitalasset.walletservice.entity.Wallet;
import com.digitalasset.walletservice.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public WalletDto createWallet(Long userId, AssetType assetType) {
        walletRepository.findByUserIdAndAssetType(userId, assetType).ifPresent(w -> {
            throw BusinessException.conflict(
                    "Wallet already exists for user " + userId + " and asset type " + assetType);
        });

        String address = generateWalletAddress();
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .walletAddress(address)
                .assetType(assetType)
                .balance(BigDecimal.ZERO)
                .build();

        Wallet saved = walletRepository.save(wallet);
        log.info("Created wallet {} for user {} asset {}", address, userId, assetType);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public WalletDto getWallet(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> BusinessException.notFound("Wallet", walletId));
        return toDto(wallet);
    }

    @Transactional(readOnly = true)
    public List<WalletDto> getUserWallets(Long userId) {
        return walletRepository.findByUserId(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WalletDto getBalance(String walletAddress) {
        Wallet wallet = walletRepository.findByWalletAddress(walletAddress)
                .orElseThrow(() -> BusinessException.notFound("Wallet", walletAddress));
        return toDto(wallet);
    }

    @Transactional
    public void creditWallet(Long walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> BusinessException.notFound("Wallet", walletId));
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
        log.debug("Credited {} to wallet {}", amount, walletId);
    }

    @Transactional
    public void debitWallet(Long walletId, BigDecimal amount) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> BusinessException.notFound("Wallet", walletId));
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw BusinessException.badRequest("Insufficient balance in wallet " + walletId);
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        log.debug("Debited {} from wallet {}", amount, walletId);
    }

    @Transactional
    public void transfer(String fromAddress, String toAddress, BigDecimal amount,
                         Long fromUserId, Long toUserId, String memo, String blockchainTxHash) {
        Wallet from = walletRepository.findByWalletAddressForUpdate(fromAddress)
                .orElseThrow(() -> BusinessException.notFound("Wallet", fromAddress));
        Wallet to = walletRepository.findByWalletAddressForUpdate(toAddress)
                .orElseThrow(() -> BusinessException.notFound("Wallet", toAddress));

        if (!from.getAssetType().equals(to.getAssetType())) {
            throw BusinessException.badRequest("Cannot transfer between different asset types");
        }
        if (from.getBalance().compareTo(amount) < 0) {
            throw BusinessException.badRequest("Insufficient balance: available "
                    + from.getBalance() + ", required " + amount);
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        walletRepository.save(from);
        walletRepository.save(to);

        TokenTransferredEvent event = TokenTransferredEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .assetType(from.getAssetType())
                .amount(amount)
                .fromWalletAddress(fromAddress)
                .toWalletAddress(toAddress)
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .blockchainTxHash(blockchainTxHash)
                .memo(memo)
                .build();

        kafkaTemplate.send(TokenTransferredEvent.TOPIC, event.getEventId(), event);
        log.info("Transferred {} {} from {} to {}", amount, from.getAssetType(), fromAddress, toAddress);
    }

    private String generateWalletAddress() {
        byte[] bytes = new byte[20];
        secureRandom.nextBytes(bytes);
        return "0x" + HexFormat.of().formatHex(bytes);
    }

    private WalletDto toDto(Wallet wallet) {
        return WalletDto.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .walletAddress(wallet.getWalletAddress())
                .assetType(wallet.getAssetType())
                .balance(wallet.getBalance())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
}
