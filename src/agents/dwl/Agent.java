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
    
    // Surprise reaction system (Phase 3)
    private int panicCounter = 0;
    private int[] lastEnemyPositions = new int[10]; // Track up to 10 enemy positions
    private int lastEnemyCount = 0;
    
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
        
        // Handle panic reaction from surprise enemies (Phase 3)
        if (panicCounter > 0) {
            panicCounter--;
            // During panic, make erratic movements
            return getPanicActions();
        }
        
        // Check for surprise enemies that trigger panic
        if (checkForSurpriseEnemies(model)) {
            panicCounter = emotionSystem.getPanicDuration();
            return getPanicActions(); // Immediate panic reaction
        }
        
        // Handle hesitation behavior
        if (hesitationCounter > 0) {
            hesitationCounter--;
            // During hesitation, don't move (but can still look around)
            return actions;
        }
        
        // Check if we should hesitate before making risky moves (Phase 3: Emotion-based)
        if (emotionSystem.shouldHesitate() && isRiskyMove(model)) {
            hesitationCounter = emotionSystem.getHesitationDuration();
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
        
        // Enhanced mistake system (Phase 3: Emotion-based)
        if (emotionSystem.shouldMakeMistake()) {
            applyRealisticMistake();
        }
        
        return actions;
    }
    
    @Override
    public String getAgentName() {
        return "HumanLikeMarioAgent_dwl";
    }
    
    /**
     * Initialize all FSM state implementations (Phase 3: All states)
     */
    private void initializeStates() {
        stateImplementations = new FSMState[GameState.values().length];
        stateImplementations[GameState.EXPLORING.ordinal()] = new ExploringState();
        stateImplementations[GameState.JUMPING.ordinal()] = new JumpingState();
        stateImplementations[GameState.COLLECTING.ordinal()] = new CollectingState();
        stateImplementations[GameState.FLEEING.ordinal()] = new FleeingState();
        stateImplementations[GameState.STUCK.ordinal()] = new StuckState();
        stateImplementations[GameState.HESITATING.ordinal()] = new HesitatingState();
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
     * Check if the next move would be risky (Phase 3: Enhanced risk assessment)
     */
    private boolean isRiskyMove(MarioForwardModel model) {
        int[][] observation = model.getMarioSceneObservation();
        int[][] enemies = model.getMarioEnemiesObservation();
        int marioRow = observation.length / 2;
        int marioCol = observation[0].length / 2;
        
        // Check for large gaps ahead
        int gapWidth = measureGapWidth(observation, marioRow, marioCol);
        if (gapWidth > 2) return true; // Wide gaps are risky
        
        // Check for high walls/obstacles
        int obstacleHeight = measureObstacleHeight(observation, marioRow, marioCol);
        if (obstacleHeight > 3) return true; // High obstacles are risky
        
        // Check for enemies nearby
        if (hasEnemiesNearby(enemies, marioRow, marioCol)) return true;
        
        // Check for complex terrain (multiple obstacles in succession)
        if (hasComplexTerrain(observation, marioRow, marioCol)) return true;
        
        return false;
    }
    
    /**
     * Measure width of gap ahead
     */
    private int measureGapWidth(int[][] observation, int marioRow, int marioCol) {
        int gapWidth = 0;
        for (int c = marioCol + 1; c < Math.min(observation[0].length, marioCol + 6); c++) {
            boolean hasGround = false;
            for (int r = marioRow + 1; r < observation.length; r++) {
                if (observation[r][c] != 0) {
                    hasGround = true;
                    break;
                }
            }
            if (!hasGround) {
                gapWidth++;
            } else {
                break; // End of gap
            }
        }
        return gapWidth;
    }
    
    /**
     * Measure height of obstacle ahead
     */
    private int measureObstacleHeight(int[][] observation, int marioRow, int marioCol) {
        if (marioCol + 1 >= observation[0].length) return 0;
        
        int height = 0;
        for (int r = marioRow; r >= 0; r--) {
            if (observation[r][marioCol + 1] != 0) {
                height++;
            } else {
                break;
            }
        }
        return height;
    }
    
    /**
     * Check for enemies in nearby area
     */
    private boolean hasEnemiesNearby(int[][] enemies, int marioRow, int marioCol) {
        for (int r = Math.max(0, marioRow - 2); r < Math.min(enemies.length, marioRow + 3); r++) {
            for (int c = marioCol; c < Math.min(enemies[0].length, marioCol + 4); c++) {
                if (enemies[r][c] != 0) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Check for complex terrain requiring careful navigation
     */
    private boolean hasComplexTerrain(int[][] observation, int marioRow, int marioCol) {
        int obstacleCount = 0;
        
        // Count obstacles in next 5 columns
        for (int c = marioCol + 1; c < Math.min(observation[0].length, marioCol + 6); c++) {
            for (int r = Math.max(0, marioRow - 2); r <= marioRow + 1; r++) {
                if (observation[r][c] != 0) {
                    obstacleCount++;
                    break; // Only count one obstacle per column
                }
            }
        }
        
        return obstacleCount >= 3; // 3+ obstacles in succession = complex
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
     * Apply realistic human-like mistakes (Phase 3: Enhanced)
     */
    private void applyRealisticMistake() {
        float severity = emotionSystem.getMistakeSeverity();
        
        // Type 1: Miss jump input (most common human mistake)
        if (actions[MarioActions.JUMP.getValue()] && Math.random() < 0.4) {
            actions[MarioActions.JUMP.getValue()] = false;
        }
        
        // Type 2: Release jump too early/late (timing mistake)
        else if (actions[MarioActions.JUMP.getValue()] && Math.random() < 0.3) {
            // This will be handled in JumpingState with mistaken timing
            // We'll add a flag for the jumping state to use
        }
        
        // Type 3: Accidentally press speed when shouldn't (panic input)
        else if (!actions[MarioActions.SPEED.getValue()] && Math.random() < 0.2 * severity) {
            actions[MarioActions.SPEED.getValue()] = true;
        }
        
        // Type 4: Forget to press speed when should (hesitation input)
        else if (actions[MarioActions.SPEED.getValue()] && Math.random() < 0.25 * severity) {
            actions[MarioActions.SPEED.getValue()] = false;
        }
        
        // Type 5: Wrong directional input momentarily
        else if (Math.random() < 0.15 * severity) {
            if (actions[MarioActions.RIGHT.getValue()]) {
                actions[MarioActions.RIGHT.getValue()] = false;
                // Brief pause (human confusion)
            }
        }
    }
    
    /**
     * Check for surprise enemies that might trigger panic reaction
     */
    private boolean checkForSurpriseEnemies(MarioForwardModel model) {
        int[][] enemies = model.getMarioEnemiesObservation();
        int marioRow = enemies.length / 2;
        int marioCol = enemies[0].length / 2;
        
        // Count current enemies in immediate vicinity
        int currentEnemyCount = 0;
        int[] currentPositions = new int[10];
        int enemyIndex = 0;
        
        for (int r = Math.max(0, marioRow - 2); r < Math.min(enemies.length, marioRow + 3) && enemyIndex < 10; r++) {
            for (int c = Math.max(0, marioCol - 2); c < Math.min(enemies[0].length, marioCol + 5) && enemyIndex < 10; c++) {
                if (enemies[r][c] != 0) {
                    currentEnemyCount++;
                    currentPositions[enemyIndex] = r * enemies[0].length + c; // Encode position
                    enemyIndex++;
                }
            }
        }
        
        // Check for surprise conditions
        boolean surpriseDetected = false;
        
        // Condition 1: Sudden appearance of new enemy
        if (currentEnemyCount > lastEnemyCount) {
            surpriseDetected = emotionSystem.shouldPanic();
        }
        
        // Condition 2: Enemy moved unexpectedly close
        if (!surpriseDetected && currentEnemyCount > 0) {
            for (int i = 0; i < enemyIndex; i++) {
                boolean wasPresent = false;
                for (int j = 0; j < Math.min(lastEnemyCount, 10); j++) {
                    if (Math.abs(currentPositions[i] - lastEnemyPositions[j]) <= 2) {
                        wasPresent = true;
                        break;
                    }
                }
                if (!wasPresent && emotionSystem.shouldPanic()) {
                    surpriseDetected = true;
                    break;
                }
            }
        }
        
        // Update tracking for next frame
        lastEnemyCount = currentEnemyCount;
        System.arraycopy(currentPositions, 0, lastEnemyPositions, 0, Math.min(10, enemyIndex));
        
        return surpriseDetected;
    }
    
    /**
     * Generate panic actions (erratic human-like behavior)
     */
    private boolean[] getPanicActions() {
        boolean[] panicActions = new boolean[actions.length];
        
        // Random erratic movement during panic
        double panicRandom = Math.random();
        
        if (panicRandom < 0.3) {
            // Panic jump
            panicActions[MarioActions.JUMP.getValue()] = true;
            panicActions[MarioActions.RIGHT.getValue()] = Math.random() < 0.7;
        } else if (panicRandom < 0.5) {
            // Freeze in panic (common human reaction)
            // All actions remain false
        } else if (panicRandom < 0.7) {
            // Panic run
            panicActions[MarioActions.RIGHT.getValue()] = true;
            panicActions[MarioActions.SPEED.getValue()] = true;
        } else {
            // Confused movement
            panicActions[MarioActions.RIGHT.getValue()] = Math.random() < 0.5;
            panicActions[MarioActions.JUMP.getValue()] = Math.random() < 0.3;
        }
        
        return panicActions;
    }
    
    // Getter methods for states to access agent properties
    public EmotionSystem getEmotionSystem() { return emotionSystem; }
    public GameState getCurrentState() { return currentState; }
    public int getStuckCounter() { return stuckCounter; }
    public int getFramesSinceProgress() { return framesSinceProgress; }
    public boolean isHesitating() { return hesitationCounter > 0; }
    public boolean isPanicking() { return panicCounter > 0; }
}