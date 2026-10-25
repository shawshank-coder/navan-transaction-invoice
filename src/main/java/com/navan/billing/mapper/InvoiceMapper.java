package com.navan.billing.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.billing.entity.InvoiceEntity;
import com.navan.billing.entity.InvoiceLineEntity;
import com.navan.billing.entity.InvoiceLineSnapshot;
import com.navan.billing.entity.InvoiceLineSnapshot.FeeLineSnapshot;
import com.navan.billing.entity.InvoiceLineSnapshot.TaxLineSnapshot;
import com.navan.billing.model.invoice.FeeRollupLine;
import com.navan.billing.model.invoice.Invoice;
import com.navan.billing.model.invoice.InvoiceDraft;
import com.navan.billing.model.invoice.InvoiceLine;
import com.navan.billing.model.invoice.TaxRollupLine;
import com.navan.billing.model.transaction.FeeLine;
import com.navan.billing.model.transaction.TaxLine;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceMapper {

    private static final TypeReference<List<TaxRollupLine>> TAX_ROLLUP = new TypeReference<>() {
    };
    private static final TypeReference<List<FeeRollupLine>> FEE_ROLLUP = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    /** Writes the header and appends one frozen line per draft line; callers clear old lines first. */
    public void applyDraft(InvoiceEntity entity, InvoiceDraft draft) {
        entity.setCurrency(draft.getCurrency());
        entity.setTotal(draft.getTotal());
        entity.setTaxRollup(objectMapper.valueToTree(draft.getTaxRollup()));
        entity.setFeeRollup(objectMapper.valueToTree(draft.getFeeRollup()));
        for (int i = 0; i < draft.getLines().size(); i++) {
            InvoiceLine draftLine = draft.getLines().get(i);
            InvoiceLineEntity line = new InvoiceLineEntity();
            line.setInvoice(entity);
            line.setTransactionId(draftLine.getTransactionId());
            line.setLineNo(i);
            line.setSnapshotVersion(InvoiceLineSnapshot.VERSION);
            line.setSnapshot(objectMapper.valueToTree(toSnapshot(draftLine)));
            entity.getLines().add(line);
        }
    }

    public Invoice toInvoice(InvoiceEntity entity) {
        return Invoice.builder()
                .id(entity.getId())
                .currency(entity.getCurrency())
                .total(entity.getTotal())
                .lines(entity.getLines().stream()
                        .map(line -> toLine(objectMapper.convertValue(line.getSnapshot(), InvoiceLineSnapshot.class)))
                        .toList())
                .taxRollup(objectMapper.convertValue(entity.getTaxRollup(), TAX_ROLLUP))
                .feeRollup(objectMapper.convertValue(entity.getFeeRollup(), FEE_ROLLUP))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static InvoiceLineSnapshot toSnapshot(InvoiceLine line) {
        return InvoiceLineSnapshot.builder()
                .transactionId(line.getTransactionId())
                .type(line.getType())
                .externalId(line.getExternalId())
                .currency(line.getCurrency())
                .occurredAt(line.getOccurredAt())
                .total(line.getTotal())
                .taxLines(line.getTaxLines().stream().map(InvoiceMapper::toTaxLineSnapshot).toList())
                .feeLines(line.getFeeLines().stream().map(InvoiceMapper::toFeeLineSnapshot).toList())
                .metadata(line.getMetadata())
                .build();
    }

    private static InvoiceLine toLine(InvoiceLineSnapshot snapshot) {
        return InvoiceLine.builder()
                .transactionId(snapshot.getTransactionId())
                .type(snapshot.getType())
                .externalId(snapshot.getExternalId())
                .currency(snapshot.getCurrency())
                .occurredAt(snapshot.getOccurredAt())
                .total(snapshot.getTotal())
                .taxLines(snapshot.getTaxLines().stream().map(InvoiceMapper::toTaxLine).toList())
                .feeLines(snapshot.getFeeLines().stream().map(InvoiceMapper::toFeeLine).toList())
                .metadata(snapshot.getMetadata())
                .build();
    }

    private static TaxLineSnapshot toTaxLineSnapshot(TaxLine tax) {
        return TaxLineSnapshot.builder().name(tax.getName()).rate(tax.getRate()).amount(tax.getAmount()).build();
    }

    private static FeeLineSnapshot toFeeLineSnapshot(FeeLine fee) {
        return FeeLineSnapshot.builder().name(fee.getName()).amount(fee.getAmount()).build();
    }

    private static TaxLine toTaxLine(TaxLineSnapshot tax) {
        return TaxLine.builder().name(tax.getName()).rate(tax.getRate()).amount(tax.getAmount()).build();
    }

    private static FeeLine toFeeLine(FeeLineSnapshot fee) {
        return FeeLine.builder().name(fee.getName()).amount(fee.getAmount()).build();
    }
}
