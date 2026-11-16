package com.secureprint.exception;
public class PolicyViolationException extends RuntimeException {
    private final ErrorCode code;
    public PolicyViolationException(ErrorCode code, String message) { super(message); this.code=code; }
    public ErrorCode getCode() { return code; }
}
