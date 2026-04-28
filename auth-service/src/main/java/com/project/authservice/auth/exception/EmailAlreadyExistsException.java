package com.project.authservice.auth.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String msg) { super(msg); }
}
