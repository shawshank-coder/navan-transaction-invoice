package com.navan.billing.repository;

import com.navan.billing.entity.InvoiceEntity;
import com.navan.billing.model.invoice.InvoiceSummary;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InvoiceEntity i where i.id = :id")
    Optional<InvoiceEntity> findByIdForUpdate(long id);

    @Query("""
            select new com.navan.billing.model.invoice.InvoiceSummary(i.id, i.currency, i.total, size(i.lines), i.createdAt)
            from InvoiceEntity i
            where i.id > :after
            order by i.id""")
    List<InvoiceSummary> findPage(long after, Pageable limit);

    /** Single DELETE; invoice_lines go with it via ON DELETE CASCADE. Returns the number of invoices removed. */
    @Modifying
    @Query("delete from InvoiceEntity i where i.id = :id")
    int deleteAndCount(long id);
}
