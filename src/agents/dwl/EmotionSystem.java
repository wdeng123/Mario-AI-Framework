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
    
    // Learning system tracking (Phase 3)
    private int totalAttempts = 0;
    private int successfulJumps = 0;
    private int failedJumps = 0;
    private int levelsCompleted = 0;
    private float experienceLevel = 0.0f; // 0.0 = novice, 1.0 = expert
    
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
     * Note: Learning system data persists across levels
     */
    public void resetEmotions() {
        confidence = 0.7f;
        caution = 0.5f;
        curiosity = 0.6f;
        consecutiveDeaths = 0;
        coinsCollected = 0;
        enemiesKilled = 0;
        justGotHit = false;
        // Learning system data (experience, attempts, etc.) persists
    }
    
    /**
     * Should Mario hesitate before making a risky move? (Phase 3: Emotion-based)
     */
    public boolean shouldHesitate() {
        // Base hesitation increases with caution and decreases with confidence
        float hesitationChance = (caution * 0.3f) + ((1.0f - confidence) * 0.2f);
        
        // Recent deaths make Mario more hesitant
        if (consecutiveDeaths > 0) {
            hesitationChance += Math.min(0.3f, consecutiveDeaths * 0.1f);
        }
        
        return Math.random() < hesitationChance;
    }
    
    /**
     * Get hesitation duration based on emotional state
     */
    public int getHesitationDuration() {
        int baseFrames = 15;
        
        // More cautious = longer hesitation
        baseFrames += (int)(caution * 20);
        
        // Low confidence = longer hesitation
        baseFrames += (int)((1.0f - confidence) * 15);
        
        // Recent deaths increase hesitation time
        baseFrames += consecutiveDeaths * 5;
        
        return Math.max(10, Math.min(60, baseFrames)); // 10-60 frames (0.4-2.4 seconds)
    }
    
    /**
     * Should Mario make a mistake? (Phase 3: Emotion-based)
     */
    public boolean shouldMakeMistake() {
        // Base mistake rate increases with low confidence and high caution
        float mistakeChance = ((1.0f - confidence) * 0.08f) + (caution * 0.03f);
        
        // Recent deaths increase mistake probability
        if (consecutiveDeaths > 0) {
            mistakeChance += Math.min(0.1f, consecutiveDeaths * 0.02f);
        }
        
        return Math.random() < mistakeChance;
    }
    
    /**
     * Get mistake severity (how much to affect timing/input)
     */
    public float getMistakeSeverity() {
        // Higher caution and lower confidence = bigger mistakes
        return ((1.0f - confidence) * 0.3f) + (caution * 0.2f);
    }
    
    /**
     * Should Mario explore for coins? (Phase 3: Emotion-based)
     */
    public boolean shouldExploreForCoins() {
        // Base exploration driven by curiosity
        float explorationChance = curiosity * 0.4f;
        
        // High confidence encourages exploration
        explorationChance += confidence * 0.15f;
        
        // Low caution encourages risk-taking for rewards
        explorationChance += (1.0f - caution) * 0.1f;
        
        return Math.random() < explorationChance;
    }
    
    /**
     * Should Mario panic when seeing unexpected enemies?
     */
    public boolean shouldPanic() {
        // Panic probability increases with caution and low confidence
        float panicChance = (caution * 0.2f) + ((1.0f - confidence) * 0.15f);
        
        // Recent deaths make Mario more jumpy
        if (consecutiveDeaths > 0) {
            panicChance += Math.min(0.25f, consecutiveDeaths * 0.08f);
        }
        
        return Math.random() < panicChance;
    }
    
    /**
     * Get panic duration when surprised by enemies
     */
    public int getPanicDuration() {
        int baseFrames = 20; // Base panic time
        
        // High caution = longer panic
        baseFrames += (int)(caution * 30);
        
        // Low confidence = longer panic
        baseFrames += (int)((1.0f - confidence) * 25);
        
        return Math.max(15, Math.min(90, baseFrames)); // 15-90 frames
    }
    
    /**
     * Record successful jump for learning system
     */
    public void recordSuccessfulJump() {
        successfulJumps++;
        updateExperienceLevel();
        
        // Successful actions build confidence
        confidence = Math.min(1.0f, confidence + 0.02f);
    }
    
    /**
     * Record failed jump for learning system
     */
    public void recordFailedJump() {
        failedJumps++;
        updateExperienceLevel();
        
        // Failed actions reduce confidence but increase caution
        confidence = Math.max(0.2f, confidence - 0.05f);
        caution = Math.min(1.0f, caution + 0.03f);
    }
    
    /**
     * Record level completion for learning system
     */
    public void recordLevelCompletion() {
        levelsCompleted++;
        totalAttempts++;
        updateExperienceLevel();
        
        // Level completion is a major confidence boost
        confidence = Math.min(1.0f, confidence + 0.15f);
        caution = Math.max(0.3f, caution - 0.05f); // Less cautious after success
    }
    
    /**
     * Record level failure for learning system
     */
    public void recordLevelFailure() {
        totalAttempts++;
        updateExperienceLevel();
        // Failure handled by death tracking
    }
    
    /**
     * Update experience level based on performance
     */
    private void updateExperienceLevel() {
        // Experience based on jump success rate and level completions
        float jumpSuccessRate = (successfulJumps + failedJumps > 0) ? 
            (float)successfulJumps / (successfulJumps + failedJumps) : 0.5f;
        
        // Combine jump success rate with level completion rate
        float levelSuccessRate = (totalAttempts > 0) ? 
            (float)levelsCompleted / totalAttempts : 0.0f;
        
        // Experience is weighted combination of both
        experienceLevel = (jumpSuccessRate * 0.7f) + (levelSuccessRate * 0.3f);
        experienceLevel = Math.max(0.0f, Math.min(1.0f, experienceLevel));
    }
    
    /**
     * Get adjusted behavior parameters based on experience
     */
    public float getExperienceAdjustedHesitation() {
        // More experienced players hesitate less in familiar situations
        return Math.max(0.1f, 1.0f - (experienceLevel * 0.4f));
    }
    
    /**
     * Get adjusted mistake probability based on experience
     */
    public float getExperienceAdjustedMistakeRate() {
        // More experienced players make fewer basic mistakes
        return Math.max(0.02f, 1.0f - (experienceLevel * 0.6f));
    }
    
    // Getters for emotional state
    public float getConfidence() { return confidence; }
    public float getCaution() { return caution; }
    public float getCuriosity() { return curiosity; }
    public int getConsecutiveDeaths() { return consecutiveDeaths; }
    
    // Getters for learning system
    public float getExperienceLevel() { return experienceLevel; }
    public int getTotalAttempts() { return totalAttempts; }
    public int getSuccessfulJumps() { return successfulJumps; }
    public int getFailedJumps() { return failedJumps; }
    public int getLevelsCompleted() { return levelsCompleted; }
    
    // Emotional state summary for debugging
    public String getEmotionalState() {
        return String.format("Confidence: %.2f, Caution: %.2f, Curiosity: %.2f, Experience: %.2f", 
                           confidence, caution, curiosity, experienceLevel);
    }
}