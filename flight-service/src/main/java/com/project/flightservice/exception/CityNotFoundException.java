package com.project.flightservice.exception;

public class CityNotFoundException extends RuntimeException {
    public CityNotFoundException(String msg) { super(msg); }
}
