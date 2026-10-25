package com.navan.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.billing.exception.ConflictException;
import com.navan.billing.model.invoice.FeeRollupLine;
import com.navan.billing.model.invoice.InvoiceDraft;
import com.navan.billing.model.invoice.InvoiceLine;
import com.navan.billing.model.invoice.TaxRollupLine;
import com.navan.billing.model.transaction.FeeLine;
import com.navan.billing.model.transaction.TaxLine;
import com.navan.billing.model.transaction.Transaction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import lombok.Value;
import org.springframework.stereotype.Component;

/** Turns stored transactions into a frozen invoice: copies lines, checks currency, rolls up taxes and fees. */
@Component
public class InvoiceSnapshotBuilder {

    private final ObjectMapper objectMapper;

    public InvoiceSnapshotBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public InvoiceDraft build(List<Transaction> transactions) {
        Set<String> currencies = new TreeSet<>();
        transactions.forEach(t -> currencies.add(t.getCurrency()));
        if (currencies.size() != 1) {
            throw new ConflictException("mixed_currency", "transactions must share one currency, got " + currencies);
        }

        List<InvoiceLine> lines = new ArrayList<>();
        Map<TaxKey, BigDecimal> taxes = new LinkedHashMap<>();
        Map<String, BigDecimal> fees = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            lines.add(InvoiceLine.builder()
                    .transactionId(t.getId())
                    .type(t.getType())
                    .externalId(t.getExternalId())
                    .currency(t.getCurrency())
                    .occurredAt(t.getOccurredAt())
                    .total(t.getTotal())
                    .taxLines(t.getTaxLines())
                    .feeLines(t.getFeeLines())
                    .metadata(objectMapper.valueToTree(t.getMetadata()))
                    .build());
            for (TaxLine tax : t.getTaxLines()) {
                taxes.merge(new TaxKey(tax.getName(), normalize(tax.getRate())), tax.getAmount(), BigDecimal::add);
            }
            for (FeeLine fee : t.getFeeLines()) {
                fees.merge(fee.getName(), fee.getAmount(), BigDecimal::add);
            }
            total = total.add(t.getTotal());
        }

        List<TaxRollupLine> taxRollup = taxes.entrySet().stream()
                .map(e -> TaxRollupLine.builder()
                        .name(e.getKey().getName())
                        .rate(e.getKey().getRate())
                        .amount(e.getValue())
                        .build())
                .toList();
        List<FeeRollupLine> feeRollup = fees.entrySet().stream()
                .map(e -> FeeRollupLine.builder().name(e.getKey()).amount(e.getValue()).build())
                .toList();

        return InvoiceDraft.builder()
                .currency(currencies.iterator().next())
                .total(total)
                .lines(List.copyOf(lines))
                .taxRollup(taxRollup)
                .feeRollup(feeRollup)
                .build();
    }

    // 0.07 and 0.070 must land in the same rollup bucket.
    private static BigDecimal normalize(BigDecimal rate) {
        return rate == null ? null : rate.stripTrailingZeros();
    }

    @Value
    private static class TaxKey {
        String name;
        BigDecimal rate;
    }
}
