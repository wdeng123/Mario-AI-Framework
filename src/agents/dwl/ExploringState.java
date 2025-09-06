package agents.dwl;

import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.MarioActions;
import engine.helper.SpriteType;

/**
 * Exploring State - Cautious forward movement with environmental scanning
 * This state represents human-like exploration behavior
 */
public class ExploringState implements FSMState {
    
    @Override
    public boolean[] getActions(MarioForwardModel model, Agent agent, MarioTimer timer) {
        boolean[] actions = new boolean[MarioActions.numberOfActions()];
        
        int[][] observation = model.getMarioSceneObservation();
        int[][] enemies = model.getMarioEnemiesObservation();
        int marioRow = observation.length / 2;
        int marioCol = observation[0].length / 2;
        
        // Default cautious forward movement
        actions[MarioActions.RIGHT.getValue()] = true;
        
        // Check for immediate obstacles requiring jumping
        if (needsToJump(observation, marioRow, marioCol)) {
            actions[MarioActions.JUMP.getValue()] = true;
        }
        
        // Check for coins or power-ups nearby - human curiosity
        if (agent.getEmotionSystem().shouldExploreForCoins() && 
            hasCollectibleNearby(observation, marioRow, marioCol)) {
            // Slightly adjust movement toward collectible
            adjustForCollectible(actions, observation, marioRow, marioCol);
        }
        
        // Cautious behavior - slow down if lots of threats ahead
        if (hasManyThreatsAhead(enemies, marioRow, marioCol)) {
            // Human-like caution - don't run at full speed
            actions[MarioActions.SPEED.getValue()] = false;
        } else {
            // Normal speed when path looks clear
            actions[MarioActions.SPEED.getValue()] = true;
        }
        
        return actions;
    }
    
    @Override
    public GameState checkTransitions(MarioForwardModel model, Agent agent) {
        int[][] observation = model.getMarioSceneObservation();
        int marioRow = observation.length / 2;
        int marioCol = observation[0].length / 2;
        
        // Transition to JUMPING if obstacle ahead (Phase 2: Simple transition logic)
        if (needsComplexJump(observation, marioRow, marioCol)) {
            return GameState.JUMPING;
        }
        
        return null; // Stay in EXPLORING
    }
    
    @Override
    public GameState getGameState() {
        return GameState.EXPLORING;
    }
    
    /**
     * Check if Mario needs to jump over simple obstacles
     */
    private boolean needsToJump(int[][] observation, int marioRow, int marioCol) {
        // Check for blocks directly in front
        if (marioCol + 1 < observation[0].length && marioRow < observation.length) {
            return observation[marioRow][marioCol + 1] != 0 || 
                   observation[marioRow + 1][marioCol + 1] != 0;
        }
        return false;
    }
    
    /**
     * Check if there are collectible items nearby
     */
    private boolean hasCollectibleNearby(int[][] observation, int marioRow, int marioCol) {
        // Scan small area around Mario for coins or question blocks
        for (int r = Math.max(0, marioRow - 2); r < Math.min(observation.length, marioRow + 3); r++) {
            for (int c = Math.max(0, marioCol); c < Math.min(observation[0].length, marioCol + 4); c++) {
                if (observation[r][c] == 2 || observation[r][c] == 9) { // Coin or question block
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Adjust movement toward collectibles (human curiosity)
     */
    private void adjustForCollectible(boolean[] actions, int[][] observation, int marioRow, int marioCol) {
        // Look for collectibles above (might need to jump)
        for (int r = Math.max(0, marioRow - 3); r < marioRow; r++) {
            for (int c = marioCol + 1; c < Math.min(observation[0].length, marioCol + 3); c++) {
                if (observation[r][c] == 2 || observation[r][c] == 9) {
                    actions[MarioActions.JUMP.getValue()] = true;
                    return;
                }
            }
        }
    }
    
    /**
     * Check for many threats ahead (multiple enemies)
     */
    private boolean hasManyThreatsAhead(int[][] enemies, int marioRow, int marioCol) {
        int threatCount = 0;
        
        // Scan area ahead of Mario
        for (int r = marioRow - 1; r < Math.min(enemies.length, marioRow + 2); r++) {
            for (int c = marioCol + 1; c < Math.min(enemies[0].length, marioCol + 5); c++) {
                if (r >= 0 && enemies[r][c] != SpriteType.NONE.getValue()) {
                    threatCount++;
                }
            }
        }
        
        return threatCount >= 2; // Multiple enemies = caution
    }
    
    /**
     * Check for immediate enemy threats requiring flee response
     */
    private boolean hasImmediateEnemyThreat(int[][] enemies, int marioRow, int marioCol) {
        // Check cells immediately around Mario
        for (int r = marioRow - 1; r <= marioRow + 1; r++) {
            for (int c = marioCol - 1; c <= marioCol + 2; c++) {
                if (r >= 0 && r < enemies.length && c >= 0 && c < enemies[0].length) {
                    if (enemies[r][c] != SpriteType.NONE.getValue()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Check for valuable collectibles worth transitioning to collecting state
     */
    private boolean hasValuableCollectible(int[][] observation, int marioRow, int marioCol) {
        // Look for power-ups or multiple coins
        int coinCount = 0;
        
        for (int r = Math.max(0, marioRow - 2); r < Math.min(observation.length, marioRow + 3); r++) {
            for (int c = marioCol + 1; c < Math.min(observation[0].length, marioCol + 6); c++) {
                if (observation[r][c] == 2) { // Coin
                    coinCount++;
                } else if (observation[r][c] == 9) { // Power-up block
                    return true;
                }
            }
        }
        
        return coinCount >= 3; // Worth collecting if 3+ coins nearby
    }
    
    /**
     * Check if complex jumping sequence is needed
     */
    private boolean needsComplexJump(int[][] observation, int marioRow, int marioCol) {
        // Check for gaps or high obstacles requiring precise jumping
        if (marioCol + 3 < observation[0].length) {
            // Look for gaps
            boolean hasGap = true;
            for (int r = marioRow + 1; r < observation.length; r++) {
                if (observation[r][marioCol + 2] != 0) {
                    hasGap = false;
                    break;
                }
            }
            if (hasGap) return true;
            
            // Look for high walls
            int wallHeight = 0;
            for (int r = marioRow; r >= 0; r--) {
                if (observation[r][marioCol + 1] != 0) {
                    wallHeight++;
                } else {
                    break;
                }
            }
            return wallHeight > 2;
        }
        return false;
    }
}