package agents.dwl;

import engine.core.MarioForwardModel;
import engine.helper.GameStatus;

/**
 * Emotion system to simulate human-like emotional responses in Mario AI
 * Tracks confidence, caution, and curiosity levels that influence behavior
 */
public class EmotionSystem {
    private float confidence = 0.7f;     // How confident Mario feels (0.0 - 1.0)
    private float caution = 0.5f;        // How cautious Mario is (0.0 - 1.0)
    private float curiosity = 0.6f;      // How curious Mario is about exploration (0.0 - 1.0)
    
    // Emotional state tracking
    private int consecutiveDeaths = 0;
    private int coinsCollected = 0;
    private int enemiesKilled = 0;
    private boolean justGotHit = false;
    
    // Constants for emotional response
    private static final float DEATH_CONFIDENCE_PENALTY = -0.2f;
    private static final float DEATH_CAUTION_INCREASE = 0.15f;
    private static final float SUCCESS_CONFIDENCE_BOOST = 0.1f;
    private static final float HIT_CAUTION_INCREASE = 0.1f;
    
    public EmotionSystem() {
        resetEmotions();
    }
    
    /**
     * Update emotional state based on current game situation
     */
    public void updateEmotions(MarioForwardModel model) {
        // Check if Mario died (affects confidence and caution)
        if (model.getGameStatus() == GameStatus.LOSE) {
            consecutiveDeaths++;
            confidence = Math.max(0.2f, confidence + DEATH_CONFIDENCE_PENALTY);
            caution = Math.min(1.0f, caution + DEATH_CAUTION_INCREASE);
        }
        
        // Check for positive experiences
        int currentCoins = model.getNumCollectedCoins();
        if (currentCoins > coinsCollected) {
            confidence = Math.min(1.0f, confidence + SUCCESS_CONFIDENCE_BOOST * 0.5f);
            coinsCollected = currentCoins;
        }
        
        int currentKills = model.getKillsTotal();
        if (currentKills > enemiesKilled) {
            confidence = Math.min(1.0f, confidence + SUCCESS_CONFIDENCE_BOOST);
            caution = Math.max(0.2f, caution - 0.05f);
            enemiesKilled = currentKills;
        }
        
        // Gradually restore emotions over time
        naturalEmotionDecay();
    }
    
    /**
     * Natural decay of extreme emotions over time
     */
    private void naturalEmotionDecay() {
        // Slowly return emotions to baseline
        if (confidence < 0.7f) {
            confidence = Math.min(0.7f, confidence + 0.001f);
        } else if (confidence > 0.7f) {
            confidence = Math.max(0.7f, confidence - 0.001f);
        }
        
        if (caution > 0.5f) {
            caution = Math.max(0.5f, caution - 0.001f);
        } else if (caution < 0.5f) {
            caution = Math.min(0.5f, caution + 0.001f);
        }
    }
    
    /**
     * Reset emotions to baseline (used when starting new level)
     */
    public void resetEmotions() {
        confidence = 0.7f;
        caution = 0.5f;
        curiosity = 0.6f;
        consecutiveDeaths = 0;
        coinsCollected = 0;
        enemiesKilled = 0;
        justGotHit = false;
    }
    
    /**
     * Should Mario hesitate before making a risky move? (Basic Phase 2 implementation)
     */
    public boolean shouldHesitate() {
        return Math.random() < 0.1f; // Simple 10% chance (Phase 3 will use emotions)
    }
    
    /**
     * Should Mario make a mistake? (Basic Phase 2 implementation)
     */
    public boolean shouldMakeMistake() {
        return Math.random() < 0.05f; // Simple 5% chance (Phase 3 will use emotions)
    }
    
    /**
     * Should Mario explore for coins? (Basic Phase 2 implementation)
     */
    public boolean shouldExploreForCoins() {
        return Math.random() < 0.2f; // Simple 20% chance (Phase 3 will use emotions)
    }
    
    // Getters for emotional state
    public float getConfidence() { return confidence; }
    public float getCaution() { return caution; }
    public float getCuriosity() { return curiosity; }
    public int getConsecutiveDeaths() { return consecutiveDeaths; }
    
    // Emotional state summary for debugging
    public String getEmotionalState() {
        return String.format("Confidence: %.2f, Caution: %.2f, Curiosity: %.2f", 
                           confidence, caution, curiosity);
    }
}