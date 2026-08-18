package com.recallops.investigation.service;

public class InvalidInvestigationTransitionException extends RuntimeException {

    public InvalidInvestigationTransitionException(String message) {
        super(message);
    }
}