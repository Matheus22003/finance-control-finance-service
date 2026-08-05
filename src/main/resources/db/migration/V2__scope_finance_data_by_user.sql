ALTER TABLE incomes
    ADD COLUMN owner_user_id UUID;

UPDATE incomes
SET owner_user_id = '7f805b46-0b56-4a5d-86eb-d4f53c92db93';

ALTER TABLE incomes
    ALTER COLUMN owner_user_id SET NOT NULL;

DROP INDEX ix_incomes_transaction_date;
CREATE INDEX ix_incomes_owner_transaction_date
    ON incomes (owner_user_id, transaction_date);

ALTER TABLE expenses
    ADD COLUMN owner_user_id UUID;

UPDATE expenses
SET owner_user_id = '7f805b46-0b56-4a5d-86eb-d4f53c92db93';

ALTER TABLE expenses
    ALTER COLUMN owner_user_id SET NOT NULL;

DROP INDEX ix_expenses_transaction_date;
DROP INDEX ix_expenses_category_transaction_date;
CREATE INDEX ix_expenses_owner_transaction_date
    ON expenses (owner_user_id, transaction_date);
CREATE INDEX ix_expenses_owner_category_transaction_date
    ON expenses (owner_user_id, category, transaction_date);
