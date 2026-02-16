package com.sickton.jgaffer.openAIService;

/**
 * Service layer that generates AI-powered explanations for tactical decisions.
 *
 * <p>Wraps the {@link OpenAIClient} and provides graceful failure handling. If the
 * OpenAI API is unavailable or returns an error, the service returns a fallback
 * message instead of propagating the exception — ensuring the tactical recommendation
 * flow is never disrupted by the explanation layer.</p>
 *
 * @see OpenAIClient
 * @author sickton
 */
public class TacticalExplanationService {

    private final OpenAIClient openAIClient;

    public TacticalExplanationService(OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
    }

    /**
     * Generates a natural language explanation for an already-decided tactic.
     * The AI does NOT influence the decision — it only explains it.
     */
    public String generateExplanation(String prompt) {

        try {
            return openAIClient.explain(prompt);
        } catch (Exception e) {
            // Fail gracefully — explanation should never break gameplay
            return """
                   AI Explanation unavailable.
                   The tactical decision was generated successfully,
                   but the explanation service could not be reached.
                   """;
        }
    }
}
