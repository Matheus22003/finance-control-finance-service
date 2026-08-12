package com.financecontrol.finance.contract.category;

import java.time.Instant;

import com.financecontrol.finance.domain.FinanceCategory;

public record CategoryResponse(
        Long id,
        String code,
        String name,
        boolean defaultCategory,
        Instant createdAt,
        Instant updatedAt) {

    public static CategoryResponse from(FinanceCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.isDefaultCategory(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
