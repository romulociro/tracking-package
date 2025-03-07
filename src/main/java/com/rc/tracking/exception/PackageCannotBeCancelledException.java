package com.rc.tracking.exception;

public class PackageCannotBeCancelledException extends RuntimeException {
    public PackageCannotBeCancelledException(String message) {
        super(message);
    }
}
