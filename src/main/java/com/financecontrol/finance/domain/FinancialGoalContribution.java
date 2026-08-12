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
import jakarta.persistence.Table;

@Entity
@Table(name = "financial_goal_contributions")
public class FinancialGoalContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "financial_goal_id", nullable = false, updatable = false)
    private UUID financialGoalId;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerUserId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "contribution_date", nullable = false)
    private LocalDate contributionDate;

    @Column(length = 200)
    private String note;

    @Column(name = "source_income_id", updatable = false)
    private UUID sourceIncomeId;

    @Column(name = "source_income_description", length = 200, updatable = false)
    private String sourceIncomeDescription;

    @Column(name = "source_income_amount", precision = 19, scale = 2, updatable = false)
    private BigDecimal sourceIncomeAmount;

    @Column(name = "source_income_transaction_date", updatable = false)
    private LocalDate sourceIncomeTransactionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private GoalContributionType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FinancialGoalContribution() {
    }

    public FinancialGoalContribution(
            UUID financialGoalId,
            UUID ownerUserId,
            BigDecimal amount,
            LocalDate contributionDate,
            String note,
            GoalContributionType type) {
        this(
                financialGoalId,
                ownerUserId,
                amount,
                contributionDate,
                note,
                type,
                null,
                null,
                null,
                null);
    }

    public FinancialGoalContribution(
            UUID financialGoalId,
            UUID ownerUserId,
            BigDecimal amount,
            LocalDate contributionDate,
            String note,
            GoalContributionType type,
            UUID sourceIncomeId,
            String sourceIncomeDescription,
            BigDecimal sourceIncomeAmount,
            LocalDate sourceIncomeTransactionDate) {
        this.financialGoalId = financialGoalId;
        this.ownerUserId = ownerUserId;
        this.amount = amount;
        this.contributionDate = contributionDate;
        this.note = note;
        this.type = type;
        this.sourceIncomeId = sourceIncomeId;
        this.sourceIncomeDescription = sourceIncomeDescription;
        this.sourceIncomeAmount = sourceIncomeAmount;
        this.sourceIncomeTransactionDate = sourceIncomeTransactionDate;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getFinancialGoalId() { return financialGoalId; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getContributionDate() { return contributionDate; }
    public String getNote() { return note; }
    public UUID getSourceIncomeId() { return sourceIncomeId; }
    public String getSourceIncomeDescription() { return sourceIncomeDescription; }
    public BigDecimal getSourceIncomeAmount() { return sourceIncomeAmount; }
    public LocalDate getSourceIncomeTransactionDate() { return sourceIncomeTransactionDate; }
    public GoalContributionType getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }
}
