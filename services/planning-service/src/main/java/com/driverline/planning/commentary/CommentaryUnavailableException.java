package com.driverline.planning.commentary;

/** commentary-service couldn't be reached or failed. Returned to the client as HTTP 503. */
public class CommentaryUnavailableException extends RuntimeException {

    public CommentaryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
