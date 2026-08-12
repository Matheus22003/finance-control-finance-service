ALTER TABLE financial_goals
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE financial_goal_contributions (
    id UUID PRIMARY KEY,
    financial_goal_id UUID NOT NULL,
    owner_user_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    contribution_date DATE NOT NULL,
    note VARCHAR(200),
    type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_goal_contributions_goal
        FOREIGN KEY (financial_goal_id) REFERENCES financial_goals (id) ON DELETE CASCADE,
    CONSTRAINT ck_goal_contributions_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_goal_contributions_type CHECK (type IN ('INITIAL', 'CONTRIBUTION'))
);

CREATE INDEX ix_goal_contributions_owner_goal_date
    ON financial_goal_contributions (
        owner_user_id,
        financial_goal_id,
        contribution_date DESC,
        created_at DESC
    );

INSERT INTO financial_goal_contributions (
    id,
    financial_goal_id,
    owner_user_id,
    amount,
    contribution_date,
    note,
    type,
    created_at
)
SELECT
    id,
    id,
    owner_user_id,
    current_amount,
    CAST(created_at AS DATE),
    'Saldo inicial',
    'INITIAL',
    created_at
FROM financial_goals
WHERE current_amount > 0;
