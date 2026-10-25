package com.navan.billing.repository;

import com.navan.billing.entity.TransactionEntity;
import com.navan.billing.model.transaction.TransactionSummary;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    @Query("select t.id from TransactionEntity t where t.externalId = :externalId")
    Optional<Long> findIdByExternalId(String externalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TransactionEntity t where t.id = :id")
    Optional<TransactionEntity> findByIdForUpdate(long id);

    /** Locks in id order so concurrent invoice writes cannot deadlock each other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TransactionEntity t where t.id in :ids order by t.id")
    List<TransactionEntity> findAllByIdForUpdate(Collection<Long> ids);

    @Query("""
            select new com.navan.billing.model.transaction.TransactionSummary(
                t.id, t.externalId, t.type, t.currency, t.occurredAt, t.total, t.createdAt)
            from TransactionEntity t
            where t.id > :after
            order by t.id""")
    List<TransactionSummary> findPage(long after, Pageable limit);
}
