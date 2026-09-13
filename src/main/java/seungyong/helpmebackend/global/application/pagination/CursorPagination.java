package seungyong.helpmebackend.global.application.pagination;

import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class CursorPagination<C> {
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private final C cursorValue;
    private final Long cursorId;
    private final int pageSize;
    private final Function<C, String> formatter;

    private CursorPagination(
            C cursorValue,
            Long cursorId,
            int pageSize,
            Function<C, String> formatter
    ) {
        this.cursorValue = cursorValue;
        this.cursorId = cursorId;
        this.pageSize = pageSize;
        this.formatter = formatter;
    }

    public static CursorPagination<OffsetDateTime> offsetDateTime(
            String cursor,
            Integer requestedSize
    ) {
        return create(cursor, requestedSize, OffsetDateTime::parse, OffsetDateTime::toString);
    }

    public static CursorPagination<LocalDate> localDate(
            String cursor,
            Integer requestedSize
    ) {
        return create(cursor, requestedSize, LocalDate::parse, LocalDate::toString);
    }

    private static <C> CursorPagination<C> create(
            String cursor,
            Integer requestedSize,
            Function<String, C> parser,
            Function<C, String> formatter
    ) {
        int pageSize = normalizePageSize(requestedSize);
        if (cursor == null || cursor.isBlank()) {
            return new CursorPagination<>(null, null, pageSize, formatter);
        }

        try {
            String raw = new String(
                    Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8
            );
            int separator = raw.lastIndexOf('|');
            if (separator <= 0 || separator == raw.length() - 1) {
                throw new IllegalArgumentException("invalid cursor shape");
            }
            return new CursorPagination<>(
                    parser.apply(raw.substring(0, separator)),
                    Long.parseLong(raw.substring(separator + 1)),
                    pageSize,
                    formatter
            );
        } catch (RuntimeException exception) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
    }

    private static int normalizePageSize(Integer requestedSize) {
        int pageSize = requestedSize == null ? DEFAULT_PAGE_SIZE : requestedSize;
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
        return pageSize;
    }

    public C cursorValue() {
        return cursorValue;
    }

    public Long cursorId() {
        return cursorId;
    }

    public int pageSize() {
        return pageSize;
    }

    public int queryLimit() {
        return pageSize + 1;
    }

    public <T> CursorPage<T> page(
            List<T> fetched,
            Function<T, C> cursorValueExtractor,
            Function<T, Long> idExtractor
    ) {
        Objects.requireNonNull(fetched, "fetched");
        Objects.requireNonNull(cursorValueExtractor, "cursorValueExtractor");
        Objects.requireNonNull(idExtractor, "idExtractor");

        boolean hasNext = fetched.size() > pageSize;
        List<T> items = fetched.subList(0, Math.min(pageSize, fetched.size()));
        String nextCursor = null;
        if (hasNext) {
            T last = items.get(items.size() - 1);
            nextCursor = encode(
                    cursorValueExtractor.apply(last),
                    idExtractor.apply(last)
            );
        }
        return new CursorPage<>(items, nextCursor, hasNext);
    }

    private String encode(C value, Long id) {
        String raw = formatter.apply(Objects.requireNonNull(value, "cursor value"))
                + "|" + Objects.requireNonNull(id, "cursor id");
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
