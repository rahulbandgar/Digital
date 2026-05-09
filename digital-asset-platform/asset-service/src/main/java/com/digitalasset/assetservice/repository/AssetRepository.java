package com.digitalasset.assetservice.repository;

import com.digitalasset.assetservice.entity.Asset;
import com.digitalasset.common.enums.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findByAssetTypeAndActiveTrue(AssetType assetType);
    List<Asset> findAllByActiveTrue();
}
