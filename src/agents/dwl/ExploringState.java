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
        
        // Check for coins or power-ups nearby - human curiosity (Phase 3: Enhanced)
        if (agent.getEmotionSystem().shouldExploreForCoins()) {
            if (hasValuableCollectibleNearby(observation, marioRow, marioCol)) {
                // Take suboptimal path for valuable items
                adjustForValuableCollectible(actions, observation, marioRow, marioCol);
            } else if (hasCollectibleNearby(observation, marioRow, marioCol)) {
                // Slightly adjust movement toward normal collectible
                adjustForCollectible(actions, observation, marioRow, marioCol);
            }
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
        int[][] enemies = model.getMarioEnemiesObservation();
        int marioRow = observation.length / 2;
        int marioCol = observation[0].length / 2;
        
        // Priority 1: FLEEING if immediate enemy threat
        if (hasImmediateEnemyThreat(enemies, marioRow, marioCol)) {
            return GameState.FLEEING;
        }
        
        // Priority 2: STUCK if Mario hasn't made progress
        if (agent.getStuckCounter() > 3) {
            return GameState.STUCK;
        }
        
        // Priority 3: HESITATING if facing risky situation and emotional state suggests caution
        if (agent.getEmotionSystem().shouldHesitate() && isVeryRisky(observation, enemies, marioRow, marioCol)) {
            return GameState.HESITATING;
        }
        
        // Priority 4: COLLECTING if valuable collectibles nearby and curious
        if (agent.getEmotionSystem().shouldExploreForCoins() && hasValuableCollectible(observation, marioRow, marioCol)) {
            return GameState.COLLECTING;
        }
        
        // Priority 5: JUMPING if complex jump needed
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
     * Check for valuable collectibles worth taking suboptimal paths (Phase 3)
     */
    private boolean hasValuableCollectibleNearby(int[][] observation, int marioRow, int marioCol) {
        // Look for power-ups or concentrated coins
        int coinCount = 0;
        boolean hasPowerUp = false;
        
        // Expanded search area for valuable items
        for (int r = Math.max(0, marioRow - 3); r < Math.min(observation.length, marioRow + 4); r++) {
            for (int c = marioCol; c < Math.min(observation[0].length, marioCol + 8); c++) {
                if (observation[r][c] == 2) { // Coin
                    coinCount++;
                } else if (observation[r][c] == 9 || observation[r][c] == 7) { // Power-up or question block
                    hasPowerUp = true;
                }
            }
        }
        
        return hasPowerUp || coinCount >= 4; // Worth detouring for power-ups or lots of coins
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
     * Adjust movement toward valuable collectibles (human-like suboptimal paths)
     */
    private void adjustForValuableCollectible(boolean[] actions, int[][] observation, int marioRow, int marioCol) {
        // Look for valuable items above (worth jumping for)
        for (int r = Math.max(0, marioRow - 4); r < marioRow; r++) {
            for (int c = marioCol; c < Math.min(observation[0].length, marioCol + 5); c++) {
                if (observation[r][c] == 9 || observation[r][c] == 7) { // Power-up blocks
                    actions[MarioActions.JUMP.getValue()] = true;
                    actions[MarioActions.SPEED.getValue()] = false; // Slow down for precision
                    return;
                }
            }
        }
        
        // Look for coin trails that require backtracking or vertical exploration
        int coinsAbove = 0;
        int coinsBelow = 0;
        
        for (int r = Math.max(0, marioRow - 3); r < marioRow; r++) {
            for (int c = Math.max(0, marioCol - 2); c < Math.min(observation[0].length, marioCol + 4); c++) {
                if (observation[r][c] == 2) coinsAbove++;
            }
        }
        
        for (int r = marioRow + 1; r < Math.min(observation.length, marioRow + 3); r++) {
            for (int c = Math.max(0, marioCol - 2); c < Math.min(observation[0].length, marioCol + 4); c++) {
                if (observation[r][c] == 2) coinsBelow++;
            }
        }
        
        // Human-like curiosity: explore coin concentrations
        if (coinsAbove >= 3) {
            actions[MarioActions.JUMP.getValue()] = true;
            actions[MarioActions.SPEED.getValue()] = false; // Careful exploration
        } else if (coinsBelow >= 2) {
            // Sometimes humans drop down for coins (risky but curious behavior)
            actions[MarioActions.RIGHT.getValue()] = false; // Stop to look around
            actions[MarioActions.SPEED.getValue()] = false;
        }
    }
    
    /**
     * Check if the situation is very risky (triggers hesitation)
     */
    private boolean isVeryRisky(int[][] observation, int[][] enemies, int marioRow, int marioCol) {
        // Multiple enemies nearby
        int enemyCount = 0;
        for (int r = Math.max(0, marioRow - 2); r <= Math.min(enemies.length - 1, marioRow + 2); r++) {
            for (int c = marioCol; c <= Math.min(enemies[0].length - 1, marioCol + 5); c++) {
                if (enemies[r][c] != 0) {
                    enemyCount++;
                }
            }
        }
        if (enemyCount >= 2) return true;
        
        // Large gap ahead
        int gapWidth = 0;
        for (int c = marioCol + 1; c < Math.min(observation[0].length, marioCol + 5); c++) {
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
                break;
            }
        }
        if (gapWidth >= 3) return true;
        
        // Very high obstacle
        int wallHeight = 0;
        if (marioCol + 1 < observation[0].length) {
            for (int r = marioRow; r >= 0; r--) {
                if (observation[r][marioCol + 1] != 0) {
                    wallHeight++;
                } else {
                    break;
                }
            }
        }
        if (wallHeight >= 4) return true;
        
        return false;
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