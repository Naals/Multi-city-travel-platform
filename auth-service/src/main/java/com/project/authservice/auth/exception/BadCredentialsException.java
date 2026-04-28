package com.project.authservice.auth.exception;

public class BadCredentialsException extends RuntimeException {
    public BadCredentialsException(String msg) { super(msg); }
}
