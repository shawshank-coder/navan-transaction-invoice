package com.navan.billing.controller;

import com.navan.billing.model.PageQuery;
import com.navan.billing.model.PageResponse;
import com.navan.billing.model.invoice.Invoice;
import com.navan.billing.model.invoice.InvoiceRequest;
import com.navan.billing.model.invoice.InvoiceSummary;
import com.navan.billing.service.IInvoiceService;
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
@RequestMapping("/invoices")
public class InvoiceController {

    private final IInvoiceService service;

    public InvoiceController(IInvoiceService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Invoice create(@RequestBody InvoiceRequest request) {
        return service.create(request.requestedIds());
    }

    @GetMapping("/{id}")
    public Invoice get(@PathVariable long id) {
        return service.get(id);
    }

    @GetMapping
    public PageResponse<InvoiceSummary> list(@RequestParam(required = false) Integer limit,
                                             @RequestParam(required = false) Long after) {
        return service.list(PageQuery.of(limit, after));
    }

    @PatchMapping("/{id}")
    public Invoice patch(@PathVariable long id, @RequestBody InvoiceRequest request) {
        return service.replaceTransactions(id, request.requestedIds());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
