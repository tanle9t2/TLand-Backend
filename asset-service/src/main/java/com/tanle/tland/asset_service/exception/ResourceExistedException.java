package com.tanle.tland.asset_service.exception;

public class ResourceExistedException  extends RuntimeException {
    public ResourceExistedException(String message) {
        super(message);
    }
}
