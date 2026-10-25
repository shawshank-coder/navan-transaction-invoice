package com.navan.billing.service.impl;

import com.navan.billing.entity.InvoiceEntity;
import com.navan.billing.exception.ConflictException;
import com.navan.billing.exception.NotFoundException;
import com.navan.billing.mapper.InvoiceMapper;
import com.navan.billing.mapper.TransactionMapper;
import com.navan.billing.model.PageQuery;
import com.navan.billing.model.PageResponse;
import com.navan.billing.model.invoice.Invoice;
import com.navan.billing.model.invoice.InvoiceDraft;
import com.navan.billing.model.invoice.InvoiceSummary;
import com.navan.billing.model.transaction.Transaction;
import com.navan.billing.repository.InvoiceLineRepository;
import com.navan.billing.repository.InvoiceRepository;
import com.navan.billing.repository.TransactionRepository;
import com.navan.billing.service.IInvoiceService;
import com.navan.billing.service.InvoiceSnapshotBuilder;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements IInvoiceService {

    private final InvoiceRepository invoices;
    private final InvoiceLineRepository invoiceLines;
    private final TransactionRepository transactions;
    private final InvoiceSnapshotBuilder snapshotBuilder;
    private final InvoiceMapper invoiceMapper;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional
    public Invoice create(List<Long> transactionIds) {
        InvoiceDraft draft = snapshotBuilder.build(lockTransactions(transactionIds, null));
        InvoiceEntity entity = new InvoiceEntity();
        invoiceMapper.applyDraft(entity, draft);
        return invoiceMapper.toInvoice(invoices.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice get(long id) {
        return invoices.findById(id).map(invoiceMapper::toInvoice).orElseThrow(() -> notFound(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InvoiceSummary> list(PageQuery page) {
        List<InvoiceSummary> rows = invoices.findPage(page.getAfter(), PageRequest.ofSize(page.getFetchSize()));
        return PageResponse.of(rows, page, InvoiceSummary::getId);
    }

    @Override
    @Transactional
    public Invoice replaceTransactions(long id, List<Long> transactionIds) {
        InvoiceEntity entity = invoices.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
        InvoiceDraft draft = snapshotBuilder.build(lockTransactions(transactionIds, id));

        entity.getLines().clear();
        // Hibernate flushes inserts before deletes; transaction_id is unique on invoice_lines, so delete old lines first.
        invoices.flush();
        invoiceMapper.applyDraft(entity, draft);
        return invoiceMapper.toInvoice(invoices.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void delete(long id) {
        if (invoices.deleteAndCount(id) == 0) {
            throw notFound(id);
        }
    }

    /**
     * Row locks stop a concurrent PATCH/DELETE or a second invoice from touching a transaction while it is being
     * snapshotted. {@code rebuiltInvoiceId} is the invoice being replaced (its own lines don't count), or null on create.
     */
    private List<Transaction> lockTransactions(List<Long> ids, Long rebuiltInvoiceId) {
        Set<Long> unique = new LinkedHashSet<>(ids);
        if (unique.size() != ids.size()) {
            throw new ConflictException("duplicate_transaction_id", "a transaction id is listed more than once");
        }
        Map<Long, Transaction> found = transactions.findAllByIdForUpdate(unique).stream()
                .map(transactionMapper::toTransaction)
                .collect(Collectors.toMap(Transaction::getId, Function.identity()));
        List<Long> missing = ids.stream().filter(id -> !found.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new NotFoundException("transactions not found: " + missing);
        }
        List<Long> alreadyInvoiced = invoiceLines.findTransactionIdsOnOtherInvoices(unique, rebuiltInvoiceId);
        if (!alreadyInvoiced.isEmpty()) {
            throw new ConflictException("transaction_already_invoiced",
                    "transactions already on another invoice: " + alreadyInvoiced);
        }
        return ids.stream().map(found::get).toList();
    }

    private static NotFoundException notFound(long id) {
        return new NotFoundException("invoice " + id + " not found");
    }
}
