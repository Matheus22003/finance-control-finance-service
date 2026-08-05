CREATE TABLE recurring_transactions (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    kind VARCHAR(10) NOT NULL,
    description VARCHAR(200) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    category VARCHAR(20),
    frequency VARCHAR(10) NOT NULL,
    start_date DATE NOT NULL,
    next_occurrence_date DATE NOT NULL,
    end_date DATE,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_recurring_transactions_kind CHECK (kind IN ('INCOME', 'EXPENSE')),
    CONSTRAINT ck_recurring_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_recurring_transactions_category CHECK (
        category IS NULL OR category IN ('FOOD', 'TRANSPORT', 'RENT', 'LEISURE', 'HEALTH', 'OTHER')
    ),
    CONSTRAINT ck_recurring_transactions_frequency CHECK (frequency IN ('WEEKLY', 'MONTHLY', 'YEARLY')),
    CONSTRAINT ck_recurring_transactions_category_by_kind CHECK (
        (kind = 'INCOME' AND category IS NULL) OR
        (kind = 'EXPENSE' AND category IS NOT NULL)
    ),
    CONSTRAINT ck_recurring_transactions_end_date CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX ix_recurring_transactions_owner_active_next
    ON recurring_transactions (owner_user_id, active, next_occurrence_date);

ALTER TABLE incomes
    ADD COLUMN recurring_transaction_id UUID;

ALTER TABLE incomes
    ADD COLUMN occurrence_date DATE;

ALTER TABLE incomes
    ADD CONSTRAINT fk_incomes_recurring_transaction
        FOREIGN KEY (recurring_transaction_id) REFERENCES recurring_transactions (id) ON DELETE SET NULL;

ALTER TABLE incomes
    ADD CONSTRAINT ck_incomes_recurring_occurrence CHECK (
        (recurring_transaction_id IS NULL AND occurrence_date IS NULL) OR
        (recurring_transaction_id IS NOT NULL AND occurrence_date IS NOT NULL)
    );

CREATE UNIQUE INDEX ux_incomes_recurring_occurrence
    ON incomes (recurring_transaction_id, occurrence_date);

ALTER TABLE expenses
    ADD COLUMN recurring_transaction_id UUID;

ALTER TABLE expenses
    ADD COLUMN occurrence_date DATE;

ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_recurring_transaction
        FOREIGN KEY (recurring_transaction_id) REFERENCES recurring_transactions (id) ON DELETE SET NULL;

ALTER TABLE expenses
    ADD CONSTRAINT ck_expenses_recurring_occurrence CHECK (
        (recurring_transaction_id IS NULL AND occurrence_date IS NULL) OR
        (recurring_transaction_id IS NOT NULL AND occurrence_date IS NOT NULL)
    );

CREATE UNIQUE INDEX ux_expenses_recurring_occurrence
    ON expenses (recurring_transaction_id, occurrence_date);

CREATE TABLE monthly_budgets (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    reference_month DATE NOT NULL,
    category VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_monthly_budgets_first_day CHECK (EXTRACT(DAY FROM reference_month) = 1),
    CONSTRAINT ck_monthly_budgets_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_monthly_budgets_category CHECK (
        category IN ('FOOD', 'TRANSPORT', 'RENT', 'LEISURE', 'HEALTH', 'OTHER')
    ),
    CONSTRAINT ux_monthly_budgets_owner_month_category
        UNIQUE (owner_user_id, reference_month, category)
);

CREATE INDEX ix_monthly_budgets_owner_month
    ON monthly_budgets (owner_user_id, reference_month);
