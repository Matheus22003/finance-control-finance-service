CREATE TABLE financial_goals (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    target_amount NUMERIC(19, 2) NOT NULL,
    current_amount NUMERIC(19, 2) NOT NULL,
    target_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_financial_goals_name_not_blank CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_financial_goals_target_positive CHECK (target_amount > 0),
    CONSTRAINT ck_financial_goals_current_non_negative CHECK (current_amount >= 0)
);

CREATE INDEX ix_financial_goals_owner_target_date
    ON financial_goals (owner_user_id, target_date);
