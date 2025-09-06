package agents.dwl;

import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.MarioActions;

/**
 * Stuck State - Repeated attempts to overcome obstacles
 * Human-like frustrated behavior when progress stalls
 */
public class StuckState implements FSMState {
    private int stuckTimer = 0;
    private int attemptCounter = 0;
    private boolean[] lastAttempt = new boolean[MarioActions.numberOfActions()];
    private float lastMarioX = 0;
    private int attemptType = 0; // 0=jump, 1=run+jump, 2=careful, 3=random
    
    @Override
    public boolean[] getActions(MarioForwardModel model, Agent agent, MarioTimer timer) {
        boolean[] actions = new boolean[MarioActions.numberOfActions()];
        
        int[][] observation = model.getMarioSceneObservation();
        int marioRow = observation.length / 2;
        int marioCol = observation[0].length / 2;
        
        // Track if we're making progress
        float currentX = model.getMarioFloatPos()[0];
        boolean madeProgress = Math.abs(currentX - lastMarioX) > 0.1f;
        
        if (madeProgress) {
            lastMarioX = currentX;
            stuckTimer = 0; // Reset stuck timer if making progress
            return getExploringActions(); // Switch to normal movement
        }
        
        // Human-like frustrated attempts when stuck
        stuckTimer++;
        
        if (stuckTimer % 30 == 0) { // Change strategy every 30 frames (1.2 seconds)
            attemptCounter++;
            attemptType = attemptCounter % 4;
        }
        
        switch (attemptType) {
            case 0: // Simple jump attempt
                actions[MarioActions.RIGHT.getValue()] = true;
                actions[MarioActions.JUMP.getValue()] = true;
                break;
                
            case 1: // Run and jump (human tries harder)
                actions[MarioActions.RIGHT.getValue()] = true;
                actions[MarioActions.JUMP.getValue()] = true;
                actions[MarioActions.SPEED.getValue()] = true;
                break;
                
            case 2: // Careful approach (human slows down)
                actions[MarioActions.RIGHT.getValue()] = stuckTimer % 60 < 30; // Slow movement
                if (stuckTimer % 40 == 20) { // Delayed jump
                    actions[MarioActions.JUMP.getValue()] = true;
                }
                break;
                
            case 3: // Frustrated random attempts
                if (Math.random() < 0.7) {
                    actions[MarioActions.RIGHT.getValue()] = true;
                }
                if (Math.random() < 0.5) {
                    actions[MarioActions.JUMP.getValue()] = true;
                }
                if (Math.random() < 0.3) {
                    actions[MarioActions.SPEED.getValue()] = true;
                }
                break;
        }
        
        // Human-like frustration - occasionally make mistakes
        if (stuckTimer > 60 && Math.random() < 0.1) {
            // Frustrated mistake - wrong timing
            if (actions[MarioActions.JUMP.getValue()]) {
                actions[MarioActions.JUMP.getValue()] = false; // Miss the jump
            }
        }
        
        // Very frustrated behavior after being stuck for a long time
        if (stuckTimer > 180 && Math.random() < 0.05) {
            // Give up temporarily (human behavior)
            for (int i = 0; i < actions.length; i++) {
                actions[i] = false;
            }
        }
        
        System.arraycopy(actions, 0, lastAttempt, 0, actions.length);
        return actions;
    }
    
    @Override
    public GameState checkTransitions(MarioForwardModel model, Agent agent) {
        int[][] observation = model.getMarioSceneObservation();
        int[][] enemies = model.getMarioEnemiesObservation();
        int marioRow = observation.length / 2;
        int marioCol = observation[0].length / 2;
        
        // Check for progress
        float currentX = model.getMarioFloatPos()[0];
        if (Math.abs(currentX - lastMarioX) > 1.0f) {
            // Made significant progress, return to normal behavior
            if (hasCollectiblesNearby(observation, marioRow, marioCol)) {
                return GameState.COLLECTING;
            } else {
                return GameState.EXPLORING;
            }
        }
        
        // Transition to FLEEING if enemies approach while stuck
        if (hasImmediateEnemyThreat(enemies, marioRow, marioCol)) {
            return GameState.FLEEING;
        }
        
        // After many attempts, switch to hesitation (reconsider approach)
        if (stuckTimer > 240 && attemptCounter > 8) {
            return GameState.HESITATING;
        }
        
        // Eventually give up and try exploring different approach
        if (stuckTimer > 400) {
            return GameState.EXPLORING;
        }
        
        return null; // Stay in STUCK
    }
    
    @Override
    public void onEnter(MarioForwardModel model, Agent agent) {
        stuckTimer = 0;
        attemptCounter = 0;
        attemptType = 0;
        lastMarioX = model.getMarioFloatPos()[0];
        
        // Being stuck affects emotions negatively
        agent.getEmotionSystem().recordFailedJump();
    }
    
    @Override
    public GameState getGameState() {
        return GameState.STUCK;
    }
    
    /**
     * Generate basic exploring actions
     */
    private boolean[] getExploringActions() {
        boolean[] actions = new boolean[MarioActions.numberOfActions()];
        actions[MarioActions.RIGHT.getValue()] = true;
        actions[MarioActions.SPEED.getValue()] = true;
        return actions;
    }
    
    /**
     * Check for immediate enemy threats
     */
    private boolean hasImmediateEnemyThreat(int[][] enemies, int marioRow, int marioCol) {
        for (int r = Math.max(0, marioRow - 1); r <= Math.min(enemies.length - 1, marioRow + 1); r++) {
            for (int c = Math.max(0, marioCol - 1); c <= Math.min(enemies[0].length - 1, marioCol + 3); c++) {
                if (enemies[r][c] != 0) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Check if there are collectibles nearby worth transitioning to
     */
    private boolean hasCollectiblesNearby(int[][] observation, int marioRow, int marioCol) {
        int collectibleCount = 0;
        
        for (int r = Math.max(0, marioRow - 2); r < Math.min(observation.length, marioRow + 3); r++) {
            for (int c = marioCol; c < Math.min(observation[0].length, marioCol + 5); c++) {
                if (observation[r][c] == 2 || observation[r][c] == 9 || observation[r][c] == 7) {
                    collectibleCount++;
                }
            }
        }
        
        return collectibleCount >= 2; // Worth collecting if multiple items
    }
}