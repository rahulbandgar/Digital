package com.digitalasset.assetservice.service;

import com.digitalasset.assetservice.dto.BurnRequest;
import com.digitalasset.assetservice.dto.TokenizeRequest;
import com.digitalasset.assetservice.entity.Asset;
import com.digitalasset.assetservice.entity.TokenOperation;
import com.digitalasset.assetservice.repository.AssetRepository;
import com.digitalasset.assetservice.repository.TokenOperationRepository;
import com.digitalasset.common.dto.MintRequest;
import com.digitalasset.common.enums.TransactionType;
import com.digitalasset.common.events.TokenBurnedEvent;
import com.digitalasset.common.events.TokenMintedEvent;
import com.digitalasset.common.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final TokenOperationRepository tokenOperationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Asset tokenize(TokenizeRequest request, String performedBy) {
        assetRepository.findByAssetTypeAndActiveTrue(request.getAssetType()).ifPresent(a -> {
            throw BusinessException.conflict("Active asset already exists for type: " + request.getAssetType());
        });

        BigDecimal totalSupply = request.getPhysicalQuantity().multiply(request.getTokenRatio());

        Asset asset = Asset.builder()
                .name(request.getName())
                .description(request.getDescription())
                .assetType(request.getAssetType())
                .physicalQuantity(request.getPhysicalQuantity())
                .totalSupply(totalSupply)
                .tokenRatio(request.getTokenRatio())
                .vaultRef(request.getVaultRef())
                .build();

        Asset saved = assetRepository.save(asset);
        log.info("Tokenized {} {} → {} tokens (by {})",
                request.getPhysicalQuantity(), request.getAssetType(), totalSupply, performedBy);
        return saved;
    }

    @Transactional
    public TokenOperation mint(MintRequest request, String performedBy, String blockchainTxHash) {
        Asset asset = assetRepository.findByAssetTypeAndActiveTrue(request.getAssetType())
                .orElseThrow(() -> BusinessException.notFound("Asset", request.getAssetType()));

        asset.setTotalSupply(asset.getTotalSupply().add(request.getAmount()));
        assetRepository.save(asset);

        TokenOperation op = TokenOperation.builder()
                .assetId(asset.getId())
                .operationType(TransactionType.MINT)
                .assetType(request.getAssetType())
                .amount(request.getAmount())
                .walletId(request.getRecipientWalletId())
                .blockchainTxHash(blockchainTxHash)
                .performedBy(performedBy)
                .reason(request.getReason())
                .build();

        TokenOperation saved = tokenOperationRepository.save(op);

        TokenMintedEvent event = TokenMintedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .assetType(request.getAssetType())
                .amount(request.getAmount())
                .recipientWalletId(request.getRecipientWalletId())
                .blockchainTxHash(blockchainTxHash)
                .mintedBy(performedBy)
                .build();

        kafkaTemplate.send(TokenMintedEvent.TOPIC, event.getEventId(), event);
        log.info("Minted {} {} tokens to wallet {} (tx={})",
                request.getAmount(), request.getAssetType(), request.getRecipientWalletId(), blockchainTxHash);
        return saved;
    }

    @Transactional
    public TokenOperation burn(BurnRequest request, String performedBy, String blockchainTxHash) {
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> BusinessException.notFound("Asset", request.getAssetId()));

        if (asset.getTotalSupply().compareTo(request.getAmount()) < 0) {
            throw BusinessException.badRequest("Burn amount exceeds total supply");
        }

        asset.setTotalSupply(asset.getTotalSupply().subtract(request.getAmount()));
        assetRepository.save(asset);

        TokenOperation op = TokenOperation.builder()
                .assetId(asset.getId())
                .operationType(TransactionType.BURN)
                .assetType(asset.getAssetType())
                .amount(request.getAmount())
                .walletId(request.getWalletId())
                .walletAddress(request.getWalletAddress())
                .blockchainTxHash(blockchainTxHash)
                .performedBy(performedBy)
                .reason(request.getReason())
                .build();

        TokenOperation saved = tokenOperationRepository.save(op);

        TokenBurnedEvent event = TokenBurnedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .assetType(asset.getAssetType())
                .amount(request.getAmount())
                .walletId(request.getWalletId())
                .walletAddress(request.getWalletAddress())
                .blockchainTxHash(blockchainTxHash)
                .burnedBy(performedBy)
                .reason(request.getReason())
                .build();

        kafkaTemplate.send(TokenBurnedEvent.TOPIC, event.getEventId(), event);
        log.info("Burned {} {} tokens from wallet {} (by {})",
                request.getAmount(), asset.getAssetType(), request.getWalletId(), performedBy);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Asset> getAllActiveAssets() {
        return assetRepository.findAllByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Asset getAsset(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Asset", id));
    }

    @Transactional(readOnly = true)
    public List<TokenOperation> getOperationsByAsset(Long assetId) {
        return tokenOperationRepository.findByAssetIdOrderByCreatedAtDesc(assetId);
    }
}
