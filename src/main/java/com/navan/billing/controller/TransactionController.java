package com.navan.billing.controller;

import com.navan.billing.model.PageQuery;
import com.navan.billing.model.PageResponse;
import com.navan.billing.model.transaction.CreateTransactionRequest;
import com.navan.billing.model.transaction.PatchTransactionRequest;
import com.navan.billing.model.transaction.Transaction;
import com.navan.billing.model.transaction.TransactionSummary;
import com.navan.billing.service.ITransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final ITransactionService transactionService;

    public TransactionController(ITransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction create(@Valid @RequestBody CreateTransactionRequest request) {
        return transactionService.create(request);
    }

    @GetMapping("/{id}")
    public Transaction get(@PathVariable long id) {
        return transactionService.get(id);
    }

    @GetMapping
    public PageResponse<TransactionSummary> list(@RequestParam(required = false) Integer limit,
                                                 @RequestParam(required = false) Long after) {
        return transactionService.list(PageQuery.of(limit, after));
    }

    @PatchMapping("/{id}")
    public Transaction patch(@PathVariable long id, @Valid @RequestBody PatchTransactionRequest request) {
        return transactionService.patch(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        transactionService.delete(id);
    }
}
