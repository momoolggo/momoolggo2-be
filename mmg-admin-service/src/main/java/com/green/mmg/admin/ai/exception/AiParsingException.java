package com.green.mmg.admin.ai.exception;

public class AiParsingException extends RuntimeException {
    public AiParsingException(String message) { super(message); }
    public AiParsingException(String message, Throwable cause) { super(message, cause); }
}
