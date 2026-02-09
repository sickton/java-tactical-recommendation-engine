package com.sickton.jgaffer.demoUI;

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
