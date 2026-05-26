package com.green.mmg.main.chatbot;

public class GeminiException extends RuntimeException {
    public GeminiException(String message) { super(message); }
    public GeminiException(String message, Throwable cause) { super(message, cause); }
}
