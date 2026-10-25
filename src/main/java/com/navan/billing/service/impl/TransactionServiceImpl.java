package com.navan.billing.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.billing.entity.TransactionEntity;
import com.navan.billing.exception.BadRequestException;
import com.navan.billing.exception.ConflictException;
import com.navan.billing.exception.DuplicateTransactionException;
import com.navan.billing.exception.NotFoundException;
import com.navan.billing.mapper.TransactionMapper;
import com.navan.billing.model.PageQuery;
import com.navan.billing.model.PageResponse;
import com.navan.billing.model.enums.TransactionType;
import com.navan.billing.model.transaction.CreateTransactionRequest;
import com.navan.billing.model.transaction.PatchTransactionRequest;
import com.navan.billing.model.transaction.Transaction;
import com.navan.billing.model.transaction.TransactionSummary;
import com.navan.billing.model.transaction.metadata.TransactionMetadata;
import com.navan.billing.repository.InvoiceLineRepository;
import com.navan.billing.repository.TransactionRepository;
import com.navan.billing.service.ITransactionService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements ITransactionService {

    private final TransactionRepository transactions;
    private final InvoiceLineRepository invoiceLines;
    private final TransactionMapper mapper;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final TransactionTemplate transactionTemplate;

    @Override
    public Transaction create(CreateTransactionRequest request) {
        // Fast path for sequential retries; the unique constraint below is what actually guarantees one row.
        transactions.findIdByExternalId(request.getExternalId()).ifPresent(id -> {
            throw new DuplicateTransactionException(request.getExternalId(), id);
        });

        TransactionMetadata metadata = parseMetadata(request.getType(), request.getMetadata());
        TransactionEntity entity = mapper.toEntity(request, metadata);
        try {
            // saveAndFlush makes a unique-key clash surface here, not at some later flush outside this catch.
            return Objects.requireNonNull(transactionTemplate.execute(
                    status -> mapper.toTransaction(transactions.saveAndFlush(entity))));
        } catch (DataIntegrityViolationException | ConcurrencyFailureException e) {
            // Lost the race to a concurrent POST with the same external_id; its row is committed now.
            long existingId = transactions.findIdByExternalId(request.getExternalId()).orElseThrow(() -> e);
            throw new DuplicateTransactionException(request.getExternalId(), existingId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Transaction get(long id) {
        return transactions.findById(id).map(mapper::toTransaction).orElseThrow(() -> notFound(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionSummary> list(PageQuery page) {
        List<TransactionSummary> rows = transactions.findPage(page.getAfter(), PageRequest.ofSize(page.getFetchSize()));
        return PageResponse.of(rows, page, TransactionSummary::getId);
    }

    @Override
    @Transactional
    public Transaction patch(long id, PatchTransactionRequest request) {
        TransactionEntity entity = transactions.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
        rejectIfInvoiced(id, "changed");

        if (request.getOccurredAt() != null) {
            entity.setOccurredAt(request.getOccurredAt());
        }
        if (request.getTotal() != null) {
            entity.setTotal(request.getTotal());
        }
        if (request.getTaxLines() != null) {
            mapper.replaceTaxLines(entity, request.getTaxLines());
        }
        if (request.getFeeLines() != null) {
            mapper.replaceFeeLines(entity, request.getFeeLines());
        }
        if (request.getMetadata() != null) {
            mapper.setMetadata(entity, parseMetadata(entity.getType(), request.getMetadata()));
        }
        return mapper.toTransaction(transactions.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void delete(long id) {
        TransactionEntity entity = transactions.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
        rejectIfInvoiced(id, "deleted");
        transactions.delete(entity);
    }

    private void rejectIfInvoiced(long id, String action) {
        if (invoiceLines.existsByTransactionId(id)) {
            throw new ConflictException("transaction_invoiced",
                    "transaction " + id + " is on an invoice and cannot be " + action);
        }
    }

    private TransactionMetadata parseMetadata(TransactionType type, JsonNode node) {
        if (node == null || !node.isObject() || node.isEmpty()) {
            throw new BadRequestException("metadata must be a non-empty object for type " + type.code());
        }
        TransactionMetadata metadata;
        try {
            metadata = objectMapper.treeToValue(node, type.metadataClass());
        } catch (JsonMappingException e) {
            throw new BadRequestException("invalid metadata for type " + type.code(), List.of(e.getOriginalMessage()));
        } catch (JsonProcessingException e) {
            throw new BadRequestException("invalid metadata for type " + type.code());
        }

        Set<ConstraintViolation<TransactionMetadata>> violations = validator.validate(metadata);
        if (!violations.isEmpty()) {
            List<String> details = violations.stream()
                    .map(v -> "metadata." + v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .toList();
            throw new BadRequestException("invalid metadata for type " + type.code(), details);
        }
        return metadata;
    }

    private static NotFoundException notFound(long id) {
        return new NotFoundException("transaction " + id + " not found");
    }
}
