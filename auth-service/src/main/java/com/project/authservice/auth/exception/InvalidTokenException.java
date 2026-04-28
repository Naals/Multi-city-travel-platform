package com.project.authservice.auth.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String msg) { super(msg); }
}
