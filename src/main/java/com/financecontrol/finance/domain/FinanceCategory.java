package com.financecontrol.finance.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "finance_categories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_finance_categories_owner_code",
                        columnNames = {"owner_user_id", "code"}),
                @UniqueConstraint(
                        name = "ux_finance_categories_owner_name",
                        columnNames = {"owner_user_id", "normalized_name"})
        })
public class FinanceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Column(nullable = false, length = 50, updatable = false)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 80)
    private String normalizedName;

    @Column(name = "is_default", nullable = false, updatable = false)
    private boolean defaultCategory;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FinanceCategory() {
    }

    public FinanceCategory(
            UUID ownerUserId,
            String code,
            String name,
            String normalizedName,
            boolean defaultCategory) {
        this.ownerUserId = ownerUserId;
        this.code = code;
        this.defaultCategory = defaultCategory;
        updateName(name, normalizedName);
    }

    public void updateName(String name, String normalizedName) {
        this.name = name;
        this.normalizedName = normalizedName;
    }

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getNormalizedName() { return normalizedName; }
    public boolean isDefaultCategory() { return defaultCategory; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
