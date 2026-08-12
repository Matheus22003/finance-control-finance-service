package com.financecontrol.finance.service;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Object id) {
        super(resourceName + " with id " + id + " was not found.");
    }
}
