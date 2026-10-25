package com.navan.billing.repository;

import com.navan.billing.entity.InvoiceLineEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLineEntity, Long> {

    /** Backed by uq_invoice_lines_transaction_id. */
    boolean existsByTransactionId(long transactionId);

    /** Which of these transactions are already billed on an invoice other than {@code rebuiltInvoiceId} (null = any). */
    @Query("""
            select l.transactionId from InvoiceLineEntity l
            where l.transactionId in :transactionIds
              and (:rebuiltInvoiceId is null or l.invoice.id <> :rebuiltInvoiceId)
            order by l.transactionId""")
    List<Long> findTransactionIdsOnOtherInvoices(Collection<Long> transactionIds, Long rebuiltInvoiceId);
}
