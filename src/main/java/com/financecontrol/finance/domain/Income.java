package com.financecontrol.finance.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "incomes")
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "recurring_transaction_id")
    private UUID recurringTransactionId;

    @Column(name = "occurrence_date")
    private LocalDate occurrenceDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Income() {
    }

    public Income(UUID ownerUserId, String description, BigDecimal amount, LocalDate transactionDate) {
        this(ownerUserId, description, amount, transactionDate, null, null);
    }

    public Income(
            UUID ownerUserId,
            String description,
            BigDecimal amount,
            LocalDate transactionDate,
            UUID recurringTransactionId,
            LocalDate occurrenceDate) {
        this.ownerUserId = ownerUserId;
        this.description = description;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.recurringTransactionId = recurringTransactionId;
        this.occurrenceDate = occurrenceDate;
    }

    public void update(String description, BigDecimal amount, LocalDate transactionDate) {
        this.description = description;
        this.amount = amount;
        this.transactionDate = transactionDate;
    }

    public void detachFromRecurrence() {
        recurringTransactionId = null;
        occurrenceDate = null;
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

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public UUID getRecurringTransactionId() {
        return recurringTransactionId;
    }

    public LocalDate getOccurrenceDate() {
        return occurrenceDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
