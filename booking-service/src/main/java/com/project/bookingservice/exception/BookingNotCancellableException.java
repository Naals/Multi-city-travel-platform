package com.project.bookingservice.exception;

public class BookingNotCancellableException extends RuntimeException {
    public BookingNotCancellableException(String m) { super(m); }
}