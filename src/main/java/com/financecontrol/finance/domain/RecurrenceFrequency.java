package com.financecontrol.finance.domain;

import java.time.LocalDate;
import java.time.YearMonth;

public enum RecurrenceFrequency {
    WEEKLY {
        @Override
        public LocalDate next(LocalDate date, LocalDate anchor) {
            return date.plusWeeks(1);
        }
    },
    MONTHLY {
        @Override
        public LocalDate next(LocalDate date, LocalDate anchor) {
            var nextMonth = YearMonth.from(date).plusMonths(1);
            return nextMonth.atDay(Math.min(anchor.getDayOfMonth(), nextMonth.lengthOfMonth()));
        }
    },
    YEARLY {
        @Override
        public LocalDate next(LocalDate date, LocalDate anchor) {
            var nextYear = date.getYear() + 1;
            var anchorMonth = YearMonth.of(nextYear, anchor.getMonth());
            return anchorMonth.atDay(Math.min(anchor.getDayOfMonth(), anchorMonth.lengthOfMonth()));
        }
    };

    public abstract LocalDate next(LocalDate date, LocalDate anchor);
}
