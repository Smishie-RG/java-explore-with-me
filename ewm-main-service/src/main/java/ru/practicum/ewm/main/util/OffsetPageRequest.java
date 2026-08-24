package ru.practicum.ewm.main.util;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class OffsetPageRequest implements Pageable {
    private final long offset;
    private final int size;
    private final Sort sort;

    public OffsetPageRequest(long offset, int size) {
        this(offset, size, Sort.unsorted());
    }

    public OffsetPageRequest(long offset, int size, Sort sort) {
        this.offset = offset;
        this.size = size;
        this.sort = sort;
    }

    @Override
    public int getPageNumber() {
        return (int) (offset / size);
    }

    @Override
    public int getPageSize() {
        return size;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageRequest(offset + size, size, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? new OffsetPageRequest(offset - size, size, sort) : first();
    }

    @Override
    public Pageable first() {
        return new OffsetPageRequest(0, size, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageRequest((long) pageNumber * size, size, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset >= size;
    }
}
