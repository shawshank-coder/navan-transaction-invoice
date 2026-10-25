package com.navan.billing.service;

import com.navan.billing.model.PageQuery;
import com.navan.billing.model.PageResponse;
import com.navan.billing.model.invoice.Invoice;
import com.navan.billing.model.invoice.InvoiceSummary;
import java.util.List;

public interface IInvoiceService {

    /** Snapshots the given transactions into a new invoice; 404 if any is missing, 409 on mixed currency. */
    Invoice create(List<Long> transactionIds);

    Invoice get(long id);

    PageResponse<InvoiceSummary> list(PageQuery page);

    /** Rebuilds the invoice snapshot from the current transactions; leaves it unchanged on any error. */
    Invoice replaceTransactions(long id, List<Long> transactionIds);

    /** Deletes the invoice and its lines; the underlying transactions are kept. */
    void delete(long id);
}
