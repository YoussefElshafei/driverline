package com.driverline.commentary.llm;

/** The language model couldn't produce a response. The message is safe to show to users. */
public class LanguageModelException extends RuntimeException {

    public LanguageModelException(String message) {
        super(message);
    }

    public LanguageModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
