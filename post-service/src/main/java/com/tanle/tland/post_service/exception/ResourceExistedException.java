package com.tanle.tland.post_service.exception;

public class ResourceExistedException  extends RuntimeException {
    public ResourceExistedException(String message) {
        super(message);
    }
}
