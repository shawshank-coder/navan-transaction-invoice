package com.navan.billing.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Frozen snapshot row: Hibernate may insert and delete it, never update it. */
@Entity
@Table(name = "invoice_lines")
@Immutable
@Getter
@Setter
@NoArgsConstructor
public class InvoiceLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
    private InvoiceEntity invoice;

    // A plain id, not an association, so reads can never join back to the live transaction.
    @Column(nullable = false, updatable = false)
    private Long transactionId;

    @Column(nullable = false, updatable = false)
    private int lineNo;

    @Column(nullable = false, updatable = false)
    private short snapshotVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    private JsonNode snapshot;
}
