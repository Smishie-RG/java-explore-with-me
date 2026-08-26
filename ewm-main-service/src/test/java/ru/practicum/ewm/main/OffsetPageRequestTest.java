package ru.practicum.ewm.main;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.ewm.main.util.OffsetPageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OffsetPageRequestTest {
    @Test
    void shouldMoveBetweenPages() {
        OffsetPageRequest request = new OffsetPageRequest(20, 10, Sort.by("id"));

        assertEquals(2, request.getPageNumber());
        assertEquals(10, request.getPageSize());
        assertEquals(20, request.getOffset());
        assertEquals(Sort.by("id"), request.getSort());
        assertTrue(request.hasPrevious());
        assertEquals(30, request.next().getOffset());
        assertEquals(10, request.previousOrFirst().getOffset());
        assertEquals(0, request.first().getOffset());
        assertEquals(40, request.withPage(4).getOffset());
    }

    @Test
    void shouldReturnFirstPageWhenPreviousDoesNotExist() {
        Pageable request = new OffsetPageRequest(0, 10);

        assertFalse(request.hasPrevious());
        assertEquals(0, request.previousOrFirst().getOffset());
    }
}
