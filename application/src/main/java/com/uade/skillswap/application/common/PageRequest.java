package com.uade.skillswap.application.common;

public record PageRequest(int number, int size) {

    public static final int MAX_SIZE = 100;
    public static final int DEFAULT_SIZE = 20;

    public PageRequest {
        if (number < 0) {
            throw new IllegalArgumentException("Page number cannot be negative: " + number);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and " + MAX_SIZE + ": " + size);
        }
    }

    public static PageRequest first() {
        return new PageRequest(0, DEFAULT_SIZE);
    }

    public long offset() {
        return (long) number * size;
    }
}
