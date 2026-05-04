package com.project.bookingservice.exception;

public class FlightNotBookableException extends RuntimeException {
    public FlightNotBookableException(String m) { super(m); }
}
