package com.navan.billing.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.navan.billing.model.enums.TransactionType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String externalId;

    @Column(nullable = false, updatable = false)
    private TransactionType type;

    @Column(nullable = false, updatable = false)
    private String currency;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private BigDecimal total;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private JsonNode metadata;

    // Batch size matches the 100-id invoice cap, so one invoice's lines load in one query per collection.
    @ElementCollection
    @BatchSize(size = 100)
    @CollectionTable(name = "transaction_tax_lines", joinColumns = @JoinColumn(name = "transaction_id"))
    @OrderColumn(name = "line_no")
    private List<TaxLineValue> taxLines = new ArrayList<>();

    @ElementCollection
    @BatchSize(size = 100)
    @CollectionTable(name = "transaction_fee_lines", joinColumns = @JoinColumn(name = "transaction_id"))
    @OrderColumn(name = "line_no")
    private List<FeeLineValue> feeLines = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
