package com.financecontrol.finance.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "recurring_transactions")
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionKind kind;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecurrenceFrequency frequency;

    @Column(name = "start_date", nullable = false, updatable = false)
    private LocalDate startDate;

    @Column(name = "next_occurrence_date", nullable = false)
    private LocalDate nextOccurrenceDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RecurringTransaction() {
    }

    public RecurringTransaction(
            UUID ownerUserId,
            TransactionKind kind,
            String description,
            BigDecimal amount,
            Category category,
            RecurrenceFrequency frequency,
            LocalDate startDate,
            LocalDate endDate) {
        this.ownerUserId = ownerUserId;
        this.kind = kind;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.frequency = frequency;
        this.startDate = startDate;
        this.nextOccurrenceDate = startDate;
        this.endDate = endDate;
        this.active = true;
    }

    public void update(
            String description,
            BigDecimal amount,
            Category category,
            LocalDate endDate,
            boolean active) {
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.endDate = endDate;
        this.active = active && (endDate == null || !nextOccurrenceDate.isAfter(endDate));
    }

    public void advance() {
        nextOccurrenceDate = frequency.next(nextOccurrenceDate, startDate);
        if (endDate != null && nextOccurrenceDate.isAfter(endDate)) {
            active = false;
        }
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
    public TransactionKind getKind() { return kind; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public Category getCategory() { return category; }
    public RecurrenceFrequency getFrequency() { return frequency; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getNextOccurrenceDate() { return nextOccurrenceDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
