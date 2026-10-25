package com.navan.billing.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.billing.entity.FeeLineValue;
import com.navan.billing.entity.TaxLineValue;
import com.navan.billing.entity.TransactionEntity;
import com.navan.billing.model.transaction.CreateTransactionRequest;
import com.navan.billing.model.transaction.FeeLine;
import com.navan.billing.model.transaction.TaxLine;
import com.navan.billing.model.transaction.Transaction;
import com.navan.billing.model.transaction.metadata.TransactionMetadata;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionMapper {

    private final ObjectMapper objectMapper;

    public TransactionEntity toEntity(CreateTransactionRequest request, TransactionMetadata metadata) {
        TransactionEntity entity = new TransactionEntity();
        entity.setExternalId(request.getExternalId());
        entity.setType(request.getType());
        entity.setCurrency(request.getCurrency());
        entity.setOccurredAt(request.getOccurredAt());
        entity.setTotal(request.getTotal());
        setMetadata(entity, metadata);
        replaceTaxLines(entity, request.getTaxLines());
        replaceFeeLines(entity, request.getFeeLines());
        return entity;
    }

    public void setMetadata(TransactionEntity entity, TransactionMetadata metadata) {
        entity.setMetadata(objectMapper.valueToTree(metadata));
    }

    public void replaceTaxLines(TransactionEntity entity, List<TaxLine> lines) {
        entity.getTaxLines().clear();
        lines.forEach(l -> entity.getTaxLines().add(new TaxLineValue(l.getName(), l.getRate(), l.getAmount())));
    }

    public void replaceFeeLines(TransactionEntity entity, List<FeeLine> lines) {
        entity.getFeeLines().clear();
        lines.forEach(l -> entity.getFeeLines().add(new FeeLineValue(l.getName(), l.getAmount())));
    }

    public Transaction toTransaction(TransactionEntity entity) {
        return Transaction.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .type(entity.getType())
                .currency(entity.getCurrency())
                .occurredAt(entity.getOccurredAt())
                .total(money(entity.getTotal()))
                .taxLines(entity.getTaxLines().stream().map(TransactionMapper::toTaxLine).toList())
                .feeLines(entity.getFeeLines().stream().map(TransactionMapper::toFeeLine).toList())
                .metadata(objectMapper.convertValue(entity.getMetadata(), entity.getType().metadataClass()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private static TaxLine toTaxLine(TaxLineValue value) {
        return TaxLine.builder()
                .name(value.getName())
                .rate(value.getRate() == null ? null : value.getRate().stripTrailingZeros())
                .amount(money(value.getAmount()))
                .build();
    }

    private static FeeLine toFeeLine(FeeLineValue value) {
        return FeeLine.builder().name(value.getName()).amount(money(value.getAmount())).build();
    }

    // Input is validated to at most 2 decimals, so this only pads (220.0 -> 220.00) and never rounds.
    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2);
    }
}
