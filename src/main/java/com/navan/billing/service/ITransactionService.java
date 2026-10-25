package com.navan.billing.service;

import com.navan.billing.model.PageQuery;
import com.navan.billing.model.PageResponse;
import com.navan.billing.model.transaction.CreateTransactionRequest;
import com.navan.billing.model.transaction.PatchTransactionRequest;
import com.navan.billing.model.transaction.Transaction;
import com.navan.billing.model.transaction.TransactionSummary;

public interface ITransactionService {

    /** Stores a new transaction; 409 with the existing id if external_id is already taken. */
    Transaction create(CreateTransactionRequest request);

    Transaction get(long id);

    PageResponse<TransactionSummary> list(PageQuery page);

    /** Applies the given fields; 409 if the transaction is on any invoice. */
    Transaction patch(long id, PatchTransactionRequest request);

    /** Deletes the transaction and its lines; 409 if it is on any invoice. */
    void delete(long id);
}
