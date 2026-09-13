package seungyong.helpmebackend.global.application.pagination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seungyong.helpmebackend.global.exception.CustomException;
import seungyong.helpmebackend.global.exception.GlobalErrorCode;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CursorPaginationTest {

    @Test
    @DisplayName("요청 크기와 다음 페이지 확인용 query limit을 구분")
    void separatesPageSizeFromQueryLimit() {
        CursorPagination<OffsetDateTime> pagination =
                CursorPagination.offsetDateTime(null, 10);

        assertThat(pagination.pageSize()).isEqualTo(10);
        assertThat(pagination.queryLimit()).isEqualTo(11);
        assertThat(pagination.cursorValue()).isNull();
        assertThat(pagination.cursorId()).isNull();
    }

    @Test
    @DisplayName("다음 페이지가 있으면 응답 크기로 자르고 마지막 응답 항목의 커서를 생성")
    void slicesLookAheadResultAndEncodesLastReturnedItem() {
        OffsetDateTime firstTime = OffsetDateTime.parse("2026-09-13T10:00:00Z");
        CursorPagination<OffsetDateTime> pagination =
                CursorPagination.offsetDateTime(null, 1);

        CursorPage<DateTimeItem> page = pagination.page(
                List.of(
                        new DateTimeItem(firstTime, 101L),
                        new DateTimeItem(firstTime, 100L)
                ),
                DateTimeItem::value,
                DateTimeItem::id
        );

        String expectedCursor = Base64.getUrlEncoder().withoutPadding().encodeToString(
                (firstTime + "|101").getBytes(StandardCharsets.UTF_8)
        );
        assertThat(page.items()).containsExactly(new DateTimeItem(firstTime, 101L));
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).isEqualTo(expectedCursor);

        CursorPagination<OffsetDateTime> decoded =
                CursorPagination.offsetDateTime(page.nextCursor(), 1);
        assertThat(decoded.cursorValue()).isEqualTo(firstTime);
        assertThat(decoded.cursorId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("LocalDate 커서와 기본 페이지 크기를 지원")
    void supportsLocalDateCursorAndDefaultSize() {
        LocalDate periodStart = LocalDate.of(2026, 9, 7);
        String cursor = Base64.getUrlEncoder().withoutPadding().encodeToString(
                (periodStart + "|401").getBytes(StandardCharsets.UTF_8)
        );
        CursorPagination<LocalDate> pagination =
                CursorPagination.localDate(cursor, null);
        CursorPage<DateItem> page = pagination.page(
                List.of(new DateItem(periodStart, 401L)), DateItem::value, DateItem::id
        );

        assertThat(pagination.pageSize()).isEqualTo(20);
        assertThat(pagination.queryLimit()).isEqualTo(21);
        assertThat(pagination.cursorValue()).isEqualTo(periodStart);
        assertThat(pagination.cursorId()).isEqualTo(401L);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    @DisplayName("잘못된 크기와 커서는 BAD_REQUEST")
    void rejectsInvalidSizeAndCursor() {
        assertBadRequest(() -> CursorPagination.offsetDateTime(null, 0));
        assertBadRequest(() -> CursorPagination.localDate(null, 101));
        assertBadRequest(() -> CursorPagination.offsetDateTime("not-a-cursor", 20));
        assertBadRequest(() -> CursorPagination.localDate(
                Base64.getUrlEncoder().withoutPadding().encodeToString(
                        "2026-09-07".getBytes(StandardCharsets.UTF_8)
                ),
                20
        ));
    }

    private void assertBadRequest(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", GlobalErrorCode.BAD_REQUEST);
    }

    private record DateTimeItem(OffsetDateTime value, Long id) {
    }

    private record DateItem(LocalDate value, Long id) {
    }
}
