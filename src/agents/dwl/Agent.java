package agents.dwl;

import engine.core.MarioAgent;
import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.MarioActions;

/**
 * Human-like Mario AI Agent using Finite State Machine
 * Designed to exhibit realistic human behaviors including hesitation, mistakes, and exploration
 * 
 * @author dwl
 */
public class Agent implements MarioAgent {
    // Core FSM components
    private GameState currentState;
    private FSMState[] stateImplementations;
    private EmotionSystem emotionSystem;
    
    // Action array for Mario controls
    private boolean[] actions;
    
    // Timing and behavior tracking
    private int hesitationCounter = 0;
    private int stuckCounter = 0;
    private float lastMarioX = 0;
    private int framesSinceProgress = 0;
    
    // Human-like behavior parameters
    private static final int HESITATION_DURATION = 15; // frames to hesitate
    private static final int STUCK_THRESHOLD = 120;    // frames before considering stuck
    private static final float MIN_PROGRESS = 0.5f;    // minimum X progress per frame
    
    @Override
    public void initialize(MarioForwardModel model, MarioTimer timer) {
        // Initialize action array
        actions = new boolean[MarioActions.numberOfActions()];
        
        // Initialize emotion system
        emotionSystem = new EmotionSystem();
        
        // Initialize FSM states
        initializeStates();
        
        // Start in EXPLORING state
        currentState = GameState.EXPLORING;
        
        // Initialize tracking variables
        lastMarioX = model.getMarioFloatPos()[0];
        framesSinceProgress = 0;
        hesitationCounter = 0;
        stuckCounter = 0;
    }
    
    @Override
    public boolean[] getActions(MarioForwardModel model, MarioTimer timer) {
        // Clear previous actions
        clearActions();
        
        // Update emotion system based on current game state
        emotionSystem.updateEmotions(model);
        
        // Handle hesitation behavior
        if (hesitationCounter > 0) {
            hesitationCounter--;
            // During hesitation, don't move (but can still look around)
            return actions;
        }
        
        // Check if we should hesitate before making risky moves
        if (emotionSystem.shouldHesitate() && isRiskyMove(model)) {
            hesitationCounter = HESITATION_DURATION;
            return actions; // Stay still during hesitation
        }
        
        // Update progress tracking
        updateProgressTracking(model);
        
        // Check for state transitions
        GameState nextState = stateImplementations[currentState.ordinal()].checkTransitions(model, this);
        if (nextState != null && nextState != currentState) {
            // Exit current state
            stateImplementations[currentState.ordinal()].onExit(model, this);
            
            // Change state
            currentState = nextState;
            
            // Enter new state
            stateImplementations[currentState.ordinal()].onEnter(model, this);
        }
        
        // Execute current state behavior
        actions = stateImplementations[currentState.ordinal()].getActions(model, this, timer);
        
        // Basic mistake system (Phase 3 will enhance this)
        if (Math.random() < 0.05) { // 5% chance
            applyMistake();
        }
        
        return actions;
    }
    
    @Override
    public String getAgentName() {
        return "HumanLikeMarioAgent_dwl";
    }
    
    /**
     * Initialize all FSM state implementations (Phase 2: Basic states only)
     */
    private void initializeStates() {
        stateImplementations = new FSMState[GameState.values().length];
        stateImplementations[GameState.EXPLORING.ordinal()] = new ExploringState();
        stateImplementations[GameState.JUMPING.ordinal()] = new JumpingState();
    }
    
    /**
     * Clear all actions
     */
    private void clearActions() {
        for (int i = 0; i < actions.length; i++) {
            actions[i] = false;
        }
    }
    
    /**
     * Check if the next move would be risky (big jumps, near enemies, etc.)
     */
    private boolean isRiskyMove(MarioForwardModel model) {
        // Simple risk assessment - check for gaps or enemies nearby
        int[][] observation = model.getMarioSceneObservation();
        int marioRow = observation.length / 2;
        int marioCol = observation[0].length / 2;
        
        // Check for gaps in front of Mario
        if (marioCol + 2 < observation[0].length) {
            for (int r = marioRow; r < observation.length - 1; r++) {
                if (observation[r][marioCol + 2] == 0) {
                    return true; // Gap detected
                }
            }
        }
        
        return false;
    }
    
    /**
     * Update progress tracking to detect if Mario is stuck
     */
    private void updateProgressTracking(MarioForwardModel model) {
        float currentX = model.getMarioFloatPos()[0];
        
        if (Math.abs(currentX - lastMarioX) < MIN_PROGRESS) {
            framesSinceProgress++;
        } else {
            framesSinceProgress = 0;
            lastMarioX = currentX;
        }
        
        // Update stuck counter
        if (framesSinceProgress > STUCK_THRESHOLD) {
            stuckCounter++;
        } else {
            stuckCounter = 0;
        }
    }
    
    /**
     * Apply a human-like mistake (simplified for Phase 2)
     */
    private void applyMistake() {
        // Simple mistake: occasionally miss a jump (Phase 3 will expand this)
        if (actions[MarioActions.JUMP.getValue()] && Math.random() < 0.1) {
            actions[MarioActions.JUMP.getValue()] = false;
        }
    }
    
    // Getter methods for states to access agent properties
    public EmotionSystem getEmotionSystem() { return emotionSystem; }
    public GameState getCurrentState() { return currentState; }
    public int getStuckCounter() { return stuckCounter; }
    public int getFramesSinceProgress() { return framesSinceProgress; }
    public boolean isHesitating() { return hesitationCounter > 0; }
}