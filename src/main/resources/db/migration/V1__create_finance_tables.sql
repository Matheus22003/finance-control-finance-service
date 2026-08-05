CREATE TABLE incomes (
    id UUID PRIMARY KEY,
    description VARCHAR(200) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    transaction_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_incomes_amount_positive CHECK (amount > 0)
);

CREATE INDEX ix_incomes_transaction_date
    ON incomes (transaction_date);

CREATE TABLE expenses (
    id UUID PRIMARY KEY,
    description VARCHAR(200) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    transaction_date DATE NOT NULL,
    category VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_expenses_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_expenses_category CHECK (
        category IN ('FOOD', 'TRANSPORT', 'RENT', 'LEISURE', 'HEALTH', 'OTHER')
    )
);

CREATE INDEX ix_expenses_transaction_date
    ON expenses (transaction_date);

CREATE INDEX ix_expenses_category_transaction_date
    ON expenses (category, transaction_date);
