-- Up to 100 transactions per invoice, each total up to 17 integer digits: the sum needs 19 integer digits.
ALTER TABLE invoices ALTER COLUMN total SET DATA TYPE DECIMAL(21, 2);
