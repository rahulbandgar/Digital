package com.digitalasset.assetservice.repository;

import com.digitalasset.assetservice.entity.TokenOperation;
import com.digitalasset.common.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TokenOperationRepository extends JpaRepository<TokenOperation, Long> {
    List<TokenOperation> findByAssetIdOrderByCreatedAtDesc(Long assetId);
    List<TokenOperation> findByWalletIdOrderByCreatedAtDesc(Long walletId);
    List<TokenOperation> findByOperationTypeOrderByCreatedAtDesc(TransactionType operationType);
}
