package com.navan.billing.model;

import java.util.List;
import java.util.function.ToLongFunction;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PageResponse<T> {

    List<T> items;
    Long nextCursor;

    public static <T> PageResponse<T> of(List<T> rows, PageQuery query, ToLongFunction<T> idOf) {
        if (rows.size() <= query.getLimit()) {
            return new PageResponse<>(rows, null);
        }
        List<T> page = List.copyOf(rows.subList(0, query.getLimit()));
        return new PageResponse<>(page, idOf.applyAsLong(page.getLast()));
    }
}
