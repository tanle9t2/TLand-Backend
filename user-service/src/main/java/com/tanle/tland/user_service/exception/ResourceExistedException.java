package com.tanle.tland.user_service.exception;

public class ResourceExistedException  extends RuntimeException {
    public ResourceExistedException(String message) {
        super(message);
    }
}
