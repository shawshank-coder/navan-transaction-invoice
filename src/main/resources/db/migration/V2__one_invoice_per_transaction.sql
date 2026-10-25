-- A transaction may be billed on at most one invoice. The unique index also serves the "is this transaction billed?" lookup.
ALTER TABLE invoice_lines ADD CONSTRAINT uq_invoice_lines_transaction_id UNIQUE (transaction_id);
DROP INDEX ix_invoice_lines_transaction_id;
