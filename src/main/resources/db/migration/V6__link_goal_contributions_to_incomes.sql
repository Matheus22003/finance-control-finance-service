ALTER TABLE financial_goal_contributions
    ADD COLUMN source_income_id UUID;

ALTER TABLE financial_goal_contributions
    ADD COLUMN source_income_description VARCHAR(200);

ALTER TABLE financial_goal_contributions
    ADD COLUMN source_income_amount NUMERIC(19, 2);

ALTER TABLE financial_goal_contributions
    ADD COLUMN source_income_transaction_date DATE;

ALTER TABLE financial_goal_contributions
    ADD CONSTRAINT fk_goal_contributions_source_income
        FOREIGN KEY (source_income_id) REFERENCES incomes (id) ON DELETE SET NULL;

ALTER TABLE financial_goal_contributions
    ADD CONSTRAINT ck_goal_contributions_source_snapshot
        CHECK (
            (
                source_income_description IS NULL
                AND source_income_amount IS NULL
                AND source_income_transaction_date IS NULL
            )
            OR
            (
                source_income_description IS NOT NULL
                AND source_income_amount IS NOT NULL
                AND source_income_amount > 0
                AND source_income_transaction_date IS NOT NULL
            )
        );

CREATE INDEX ix_goal_contributions_source_income
    ON financial_goal_contributions (source_income_id);
