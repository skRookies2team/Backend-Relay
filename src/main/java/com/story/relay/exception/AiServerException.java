package com.story.relay.exception;

/**
 * Custom exception for AI server communication errors
 * Provides better error context than generic RuntimeException
 */
public class AiServerException extends RuntimeException {

    private final String serverType;

    public AiServerException(String serverType, String message) {
        super(String.format("[%s] %s", serverType, message));
        this.serverType = serverType;
    }

    public AiServerException(String serverType, String message, Throwable cause) {
        super(String.format("[%s] %s", serverType, message), cause);
        this.serverType = serverType;
    }

    public String getServerType() {
        return serverType;
    }
}
