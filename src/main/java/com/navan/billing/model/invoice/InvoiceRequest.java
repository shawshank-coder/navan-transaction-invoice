package com.navan.billing.model.invoice;

import com.navan.billing.exception.BadRequestException;
import java.util.List;
import lombok.Value;

@Value
public class InvoiceRequest {

    /** Bounds how many transaction rows one request can lock FOR UPDATE. */
    public static final int MAX_TRANSACTIONS_PER_INVOICE = 100;

    Long transactionId;
    List<Long> transactionIds;

    public List<Long> requestedIds() {
        if ((transactionId == null) == (transactionIds == null)) {
            throw new BadRequestException("provide exactly one of transaction_id or transaction_ids");
        }
        if (transactionId != null) {
            return List.of(transactionId);
        }
        if (transactionIds.isEmpty() || transactionIds.contains(null)) {
            throw new BadRequestException("transaction_ids must be a non-empty list of ids");
        }
        if (transactionIds.size() > MAX_TRANSACTIONS_PER_INVOICE) {
            throw new BadRequestException(
                    "transaction_ids may contain at most " + MAX_TRANSACTIONS_PER_INVOICE + " ids");
        }
        return transactionIds;
    }
}
