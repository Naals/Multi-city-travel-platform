package com.project.authservice.auth.exception;

public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException(String msg) { super(msg); }
}
