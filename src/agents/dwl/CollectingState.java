package agents.dwl;

import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.MarioActions;

/**
 * Collecting State - Focused collection of coins and power-ups
 * Human-like behavior when valuable items are nearby
 */
public class CollectingState implements FSMState {
    private int collectingTimer = 0;
    private int targetRow = -1;
    private int targetCol = -1;
    
    @Override
    public boolean[] getActions(MarioForwardModel model, Agent agent, MarioTimer timer) {
        boolean[] actions = new boolean[MarioActions.numberOfActions()];
        
        int[][] observation = model.getMarioSceneObservation();
        int marioRow = observation.length / 2;
        int marioCol = observation[0].length / 2;
        
        // Find nearest valuable collectible
        if (targetRow == -1 || targetCol == -1) {
            findNearestCollectible(observation, marioRow, marioCol);
        }
        
        // Human-like collection behavior
        if (targetRow != -1 && targetCol != -1) {
            // Move toward target with human-like precision issues
            if (targetCol > marioCol) {
                actions[MarioActions.RIGHT.getValue()] = true;
                // Sometimes overshoot when excited about collectibles
                if (agent.getEmotionSystem().getCuriosity() > 0.8f && Math.random() < 0.1) {
                    actions[MarioActions.SPEED.getValue()] = true;
                }
            } else if (targetCol < marioCol) {
                // Rarely move left in Mario, but humans sometimes do for valuable items
                actions[MarioActions.RIGHT.getValue()] = false;
            }
            
            // Jump for elevated collectibles
            if (targetRow < marioRow) {
                actions[MarioActions.JUMP.getValue()] = true;
                // Human-like timing variation
                if (Math.random() < 0.15) {
                    // Sometimes jump too early or late
                    if (Math.random() < 0.5) {
                        actions[MarioActions.JUMP.getValue()] = false; // Too late
                    }
                }
            }
        } else {
            // No target found, default movement
            actions[MarioActions.RIGHT.getValue()] = true;
            actions[MarioActions.SPEED.getValue()] = false; // Slow and careful
        }
        
        // Human-like distractedness - sometimes get distracted during collection
        if (Math.random() < 0.05) {
            // Brief pause (human gets distracted)
            for (int i = 0; i < actions.length; i++) {
                actions[i] = false;
            }
        }
        
        collectingTimer++;
        return actions;
    }
    
    @Override
    public GameState checkTransitions(MarioForwardModel model, Agent agent) {
        int[][] observation = model.getMarioSceneObservation();
        int[][] enemies = model.getMarioEnemiesObservation();
        int marioRow = observation.length / 2;
        int marioCol = observation[0].length / 2;
        
        // Transition to FLEEING if enemies get too close
        if (hasImmediateThreat(enemies, marioRow, marioCol)) {
            return GameState.FLEEING;
        }
        
        // Transition to JUMPING if obstacle requires complex navigation
        if (needsComplexJump(observation, marioRow, marioCol)) {
            return GameState.JUMPING;
        }
        
        // Return to EXPLORING if no more collectibles or took too long
        if (collectingTimer > 120 || !hasCollectiblesNearby(observation, marioRow, marioCol)) {
            return GameState.EXPLORING;
        }
        
        return null; // Stay in COLLECTING
    }
    
    @Override
    public void onEnter(MarioForwardModel model, Agent agent) {
        collectingTimer = 0;
        targetRow = -1;
        targetCol = -1;
    }
    
    @Override
    public GameState getGameState() {
        return GameState.COLLECTING;
    }
    
    /**
     * Find the nearest valuable collectible
     */
    private void findNearestCollectible(int[][] observation, int marioRow, int marioCol) {
        int closestDistance = Integer.MAX_VALUE;
        
        for (int r = Math.max(0, marioRow - 4); r < Math.min(observation.length, marioRow + 5); r++) {
            for (int c = marioCol; c < Math.min(observation[0].length, marioCol + 8); c++) {
                if (observation[r][c] == 2 || observation[r][c] == 9 || observation[r][c] == 7) {
                    int distance = Math.abs(r - marioRow) + Math.abs(c - marioCol);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        targetRow = r;
                        targetCol = c;
                    }
                }
            }
        }
    }
    
    /**
     * Check for immediate enemy threats
     */
    private boolean hasImmediateThreat(int[][] enemies, int marioRow, int marioCol) {
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
     * Check if complex jump is needed
     */
    private boolean needsComplexJump(int[][] observation, int marioRow, int marioCol) {
        // Look for high obstacles or gaps
        if (marioCol + 2 < observation[0].length) {
            // Check for obstacles
            int obstacleHeight = 0;
            for (int r = marioRow; r >= 0; r--) {
                if (observation[r][marioCol + 1] != 0) {
                    obstacleHeight++;
                } else {
                    break;
                }
            }
            
            if (obstacleHeight > 2) return true;
            
            // Check for gaps
            boolean hasGround = false;
            for (int r = marioRow + 1; r < observation.length; r++) {
                if (observation[r][marioCol + 2] != 0) {
                    hasGround = true;
                    break;
                }
            }
            if (!hasGround) return true;
        }
        return false;
    }
    
    /**
     * Check if there are still collectibles nearby
     */
    private boolean hasCollectiblesNearby(int[][] observation, int marioRow, int marioCol) {
        for (int r = Math.max(0, marioRow - 3); r < Math.min(observation.length, marioRow + 4); r++) {
            for (int c = marioCol; c < Math.min(observation[0].length, marioCol + 6); c++) {
                if (observation[r][c] == 2 || observation[r][c] == 9 || observation[r][c] == 7) {
                    return true;
                }
            }
        }
        return false;
    }
}