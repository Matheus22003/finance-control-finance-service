package com.financecontrol.finance.service;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, UUID id) {
        super(resourceName + " with id " + id + " was not found.");
    }
}
