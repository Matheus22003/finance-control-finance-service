package com.financecontrol.finance.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.financecontrol.finance.domain.FinanceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceCategoryRepository extends JpaRepository<FinanceCategory, Long> {

    List<FinanceCategory> findAllByOwnerUserIdOrderByDefaultCategoryDescNameAsc(UUID ownerUserId);

    Optional<FinanceCategory> findByIdAndOwnerUserId(Long id, UUID ownerUserId);

    Optional<FinanceCategory> findByOwnerUserIdAndCode(UUID ownerUserId, String code);

    boolean existsByOwnerUserIdAndCode(UUID ownerUserId, String code);

    boolean existsByOwnerUserIdAndNormalizedName(UUID ownerUserId, String normalizedName);

    boolean existsByOwnerUserIdAndNormalizedNameAndIdNot(
            UUID ownerUserId,
            String normalizedName,
            Long id);

    long deleteByOwnerUserId(UUID ownerUserId);

}
