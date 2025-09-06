package agents.dwl;

import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.MarioActions;

/**
 * Hesitating State - Careful assessment before risky moves
 * Human-like cautious behavior when facing uncertainty
 */
public class HesitatingState implements FSMState {
    private int hesitationTimer = 0;
    private int assessmentPhase = 0; // 0=looking, 1=testing, 2=deciding
    private boolean hasAssessed = false;
    private boolean foundSafePath = false;
    
    @Override
    public boolean[] getActions(MarioForwardModel model, Agent agent, MarioTimer timer) {
        boolean[] actions = new boolean[MarioActions.numberOfActions()];
        
        int[][] observation = model.getMarioSceneObservation();
        int[][] enemies = model.getMarioEnemiesObservation();
        int marioRow = observation.length / 2;
        int marioCol = observation[0].length / 2;
        
        hesitationTimer++;
        
        // Human-like hesitation behavior in phases
        if (hesitationTimer < 30) {
            // Phase 1: Looking around (no movement)
            assessmentPhase = 0;
            // Stay still and observe
            
        } else if (hesitationTimer < 60) {
            // Phase 2: Small test movements
            assessmentPhase = 1;
            
            // Cautious small movements to test terrain
            if (hesitationTimer % 20 < 10) {
                actions[MarioActions.RIGHT.getValue()] = true;
                actions[MarioActions.SPEED.getValue()] = false; // No running while testing
            }
            
            // Test jump occasionally
            if (hesitationTimer % 30 == 25) {
                actions[MarioActions.JUMP.getValue()] = true;
            }
            
        } else {
            // Phase 3: Cautious decision making
            assessmentPhase = 2;
            hasAssessed = true;
            
            // Assess the situation and choose action
            boolean isRisky = isPathRisky(observation, enemies, marioRow, marioCol);
            
            if (!isRisky || agent.getEmotionSystem().getConfidence() > 0.7f) {
                // Proceed cautiously
                actions[MarioActions.RIGHT.getValue()] = true;
                
                // Use speed based on confidence
                if (agent.getEmotionSystem().getConfidence() > 0.8f) {
                    actions[MarioActions.SPEED.getValue()] = true;
                }
                
                // Jump if necessary
                if (needsJump(observation, marioRow, marioCol)) {
                    actions[MarioActions.JUMP.getValue()] = true;
                }
                
                foundSafePath = true;
            } else {
                // Still too risky, continue hesitating
                if (hesitationTimer % 40 < 20) {
                    // Slow cautious movement
                    actions[MarioActions.RIGHT.getValue()] = true;
                    actions[MarioActions.SPEED.getValue()] = false;
                }
                
                // Nervous jumping
                if (Math.random() < 0.1) {
                    actions[MarioActions.JUMP.getValue()] = true;
                }
            }
        }
        
        // Human-like nervous behavior during hesitation
        if (Math.random() < 0.05) {
            // Brief nervous pause
            for (int i = 0; i < actions.length; i++) {
                actions[i] = false;
            }
        }
        
        return actions;
    }
    
    @Override
    public GameState checkTransitions(MarioForwardModel model, Agent agent) {
        int[][] observation = model.getMarioSceneObservation();
        int[][] enemies = model.getMarioEnemiesObservation();
        int marioRow = observation.length / 2;
        int marioCol = observation[0].length / 2;
        
        // Emergency transition to FLEEING if enemies get too close
        if (hasImmediateEnemyThreat(enemies, marioRow, marioCol)) {
            return GameState.FLEEING;
        }
        
        // Transition based on assessment outcome
        if (hasAssessed && foundSafePath) {
            // Found safe path, proceed with appropriate action
            if (needsComplexJump(observation, marioRow, marioCol)) {
                return GameState.JUMPING;
            } else if (hasValuableCollectibles(observation, marioRow, marioCol)) {
                return GameState.COLLECTING;
            } else {
                return GameState.EXPLORING;
            }
        }
        
        // If hesitating too long without finding solution, try different approach
        if (hesitationTimer > 120) {
            if (agent.getStuckCounter() > 0) {
                return GameState.STUCK; // We're stuck and hesitating isn't helping
            } else {
                // Force action after long hesitation
                return GameState.JUMPING;
            }
        }
        
        return null; // Stay in HESITATING
    }
    
    @Override
    public void onEnter(MarioForwardModel model, Agent agent) {
        hesitationTimer = 0;
        assessmentPhase = 0;
        hasAssessed = false;
        foundSafePath = false;
    }
    
    @Override
    public GameState getGameState() {
        return GameState.HESITATING;
    }
    
    /**
     * Assess if the path ahead is risky
     */
    private boolean isPathRisky(int[][] observation, int[][] enemies, int marioRow, int marioCol) {
        // Check for enemies nearby
        for (int r = Math.max(0, marioRow - 2); r <= Math.min(enemies.length - 1, marioRow + 2); r++) {
            for (int c = marioCol + 1; c <= Math.min(enemies[0].length - 1, marioCol + 4); c++) {
                if (enemies[r][c] != 0) {
                    return true;
                }
            }
        }
        
        // Check for complex terrain
        int obstacleCount = 0;
        for (int c = marioCol + 1; c < Math.min(observation[0].length, marioCol + 5); c++) {
            for (int r = Math.max(0, marioRow - 2); r <= marioRow + 1; r++) {
                if (observation[r][c] != 0) {
                    obstacleCount++;
                    break;
                }
            }
        }
        
        // Check for gaps
        for (int c = marioCol + 1; c < Math.min(observation[0].length, marioCol + 4); c++) {
            boolean hasGround = false;
            for (int r = marioRow + 1; r < observation.length; r++) {
                if (observation[r][c] != 0) {
                    hasGround = true;
                    break;
                }
            }
            if (!hasGround) {
                return true; // Gap detected
            }
        }
        
        return obstacleCount >= 3; // Multiple obstacles = risky
    }
    
    /**
     * Check if immediate enemy threat exists
     */
    private boolean hasImmediateEnemyThreat(int[][] enemies, int marioRow, int marioCol) {
        for (int r = Math.max(0, marioRow - 1); r <= Math.min(enemies.length - 1, marioRow + 1); r++) {
            for (int c = Math.max(0, marioCol - 1); c <= Math.min(enemies[0].length - 1, marioCol + 2); c++) {
                if (enemies[r][c] != 0) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Check if jump is needed for immediate obstacle
     */
    private boolean needsJump(int[][] observation, int marioRow, int marioCol) {
        if (marioCol + 1 < observation[0].length) {
            return observation[marioRow][marioCol + 1] != 0 || 
                   observation[marioRow + 1][marioCol + 1] != 0;
        }
        return false;
    }
    
    /**
     * Check if complex jump sequence is needed
     */
    private boolean needsComplexJump(int[][] observation, int marioRow, int marioCol) {
        // Check for high obstacles or wide gaps
        if (marioCol + 2 < observation[0].length) {
            int obstacleHeight = 0;
            for (int r = marioRow; r >= 0; r--) {
                if (observation[r][marioCol + 1] != 0) {
                    obstacleHeight++;
                } else {
                    break;
                }
            }
            return obstacleHeight > 2;
        }
        return false;
    }
    
    /**
     * Check for valuable collectibles worth pursuing
     */
    private boolean hasValuableCollectibles(int[][] observation, int marioRow, int marioCol) {
        for (int r = Math.max(0, marioRow - 3); r < Math.min(observation.length, marioRow + 4); r++) {
            for (int c = marioCol; c < Math.min(observation[0].length, marioCol + 6); c++) {
                if (observation[r][c] == 9 || observation[r][c] == 7) { // Power-ups
                    return true;
                }
            }
        }
        return false;
    }
}