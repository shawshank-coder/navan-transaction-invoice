package com.navan.billing.model;

import com.navan.billing.exception.BadRequestException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PageQuery {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 100;

    int limit;
    long after;

    public static PageQuery of(Integer limit, Long after) {
        int resolvedLimit = limit == null ? DEFAULT_LIMIT : limit;
        if (resolvedLimit < 1 || resolvedLimit > MAX_LIMIT) {
            throw new BadRequestException("limit must be between 1 and " + MAX_LIMIT);
        }
        long resolvedAfter = after == null ? 0 : after;
        if (resolvedAfter < 0) {
            throw new BadRequestException("after must be >= 0");
        }
        return new PageQuery(resolvedLimit, resolvedAfter);
    }

    /** One extra row tells us whether a next page exists without a COUNT query. */
    public int getFetchSize() {
        return limit + 1;
    }
}
