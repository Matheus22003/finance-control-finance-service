package com.financecontrol.finance.contract.recurring;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.financecontrol.finance.domain.RecurrenceFrequency;
import com.financecontrol.finance.domain.RecurringTransaction;
import com.financecontrol.finance.domain.TransactionKind;

public record RecurringTransactionResponse(
        UUID id,
        TransactionKind kind,
        String description,
        BigDecimal amount,
        String category,
        RecurrenceFrequency frequency,
        LocalDate startDate,
        LocalDate nextOccurrenceDate,
        LocalDate endDate,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static RecurringTransactionResponse from(RecurringTransaction recurring) {
        return new RecurringTransactionResponse(
                recurring.getId(),
                recurring.getKind(),
                recurring.getDescription(),
                recurring.getAmount(),
                recurring.getCategory(),
                recurring.getFrequency(),
                recurring.getStartDate(),
                recurring.getNextOccurrenceDate(),
                recurring.getEndDate(),
                recurring.isActive(),
                recurring.getCreatedAt(),
                recurring.getUpdatedAt());
    }
}
