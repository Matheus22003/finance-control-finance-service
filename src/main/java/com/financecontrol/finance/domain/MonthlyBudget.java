package com.financecontrol.finance.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
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
        name = "monthly_budgets",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_monthly_budgets_owner_month_category",
                columnNames = {"owner_user_id", "reference_month", "category"}))
public class MonthlyBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Column(name = "reference_month", nullable = false, updatable = false)
    private LocalDate referenceMonth;

    @Column(nullable = false, length = 50, updatable = false)
    private String category;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MonthlyBudget() {
    }

    public MonthlyBudget(UUID ownerUserId, YearMonth referenceMonth, String category, BigDecimal amount) {
        this.ownerUserId = ownerUserId;
        this.referenceMonth = referenceMonth.atDay(1);
        this.category = category;
        this.amount = amount;
    }

    public void updateAmount(BigDecimal amount) {
        this.amount = amount;
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

    public UUID getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public YearMonth getReferenceMonth() { return YearMonth.from(referenceMonth); }
    public String getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
