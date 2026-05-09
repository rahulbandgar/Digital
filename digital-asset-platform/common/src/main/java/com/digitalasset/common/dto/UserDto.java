package com.digitalasset.common.dto;

import com.digitalasset.common.enums.KycStatus;
import com.digitalasset.common.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private UserRole role;
    private KycStatus kycStatus;
    private Instant createdAt;
}
