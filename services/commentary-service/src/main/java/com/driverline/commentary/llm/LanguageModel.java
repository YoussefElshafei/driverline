package com.driverline.commentary.llm;

/**
 * Any text-generating language model. The commentary logic depends only on this interface, so the provider
 * (local Ollama today; a hosted model later) can be swapped without touching it.
 */
public interface LanguageModel {

    /** Short provider name shown to users, e.g. "ollama". */
    String provider();

    /** The model in use, e.g. "llama3.1:8b". */
    String model();

    /**
     * @throws LanguageModelException if the model can't be reached, times out, or returns an error
     */
    String generate(String systemPrompt, String userPrompt);
}
