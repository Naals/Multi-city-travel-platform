package com.project.bookingservice.exception;

public class BookingAlreadyCancelledException extends RuntimeException {
    public BookingAlreadyCancelledException(String m) { super(m); }
}
