package com.sd31.sunday.exception;

public class GHNException extends Exception {
    public GHNException(String message) {
        super(message);
    }

    public GHNException(String message, Throwable cause) {
        super(message, cause);
    }
}