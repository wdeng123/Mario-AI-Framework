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
        
        // Enhanced coin collection - predictive and integrated (Optimized)
        CoinTrail coinTrail = predictCoinTrail(observation, marioRow, marioCol);
        if (coinTrail != null && (agent.getEmotionSystem().shouldExploreForCoins() || coinTrail.isHighValue)) {
            // Adjust movement for predicted coin trail
            adjustForCoinTrail(actions, coinTrail, marioRow, marioCol);
        } else if (hasValuableCollectibleNearby(observation, marioRow, marioCol)) {
            // Take suboptimal path for valuable items
            adjustForValuableCollectible(actions, observation, marioRow, marioCol);
        } else if (hasCollectibleNearby(observation, marioRow, marioCol)) {
            // Slightly adjust movement toward normal collectible
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
        
        // Priority 4: COLLECTING logic now integrated - removed separate state transition
        
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
    
    /**
     * Inner class to represent a predicted coin trail
     */
    private static class CoinTrail {
        int startRow, startCol;
        int endRow, endCol;
        int coinCount;
        boolean isHighValue; // Contains power-ups or many coins
        String pattern; // "horizontal", "vertical", "arc", "cluster"
        
        CoinTrail(int startRow, int startCol, int endRow, int endCol, int coinCount, boolean isHighValue, String pattern) {
            this.startRow = startRow;
            this.startCol = startCol;
            this.endRow = endRow;
            this.endCol = endCol;
            this.coinCount = coinCount;
            this.isHighValue = isHighValue;
            this.pattern = pattern;
        }
    }
    
    /**
     * Predict coin trails ahead (2-3 tiles advance prediction)
     */
    private CoinTrail predictCoinTrail(int[][] observation, int marioRow, int marioCol) {
        // Extended search area for predictive collection
        int maxSearchCol = Math.min(observation[0].length, marioCol + 12);
        int maxSearchRow = Math.min(observation.length, marioRow + 6);
        int minSearchRow = Math.max(0, marioRow - 6);
        
        // Look for coin patterns ahead
        
        // 1. Horizontal coin trails
        CoinTrail horizontal = findHorizontalCoinTrail(observation, marioRow, marioCol, maxSearchCol);
        if (horizontal != null) return horizontal;
        
        // 2. Vertical coin trails (jumping sequences)
        CoinTrail vertical = findVerticalCoinTrail(observation, marioRow, marioCol, maxSearchCol, minSearchRow, maxSearchRow);
        if (vertical != null) return vertical;
        
        // 3. Arc-shaped coin trails (jump curves)
        CoinTrail arc = findArcCoinTrail(observation, marioRow, marioCol, maxSearchCol, minSearchRow, maxSearchRow);
        if (arc != null) return arc;
        
        // 4. Coin clusters worth detouring for
        CoinTrail cluster = findCoinCluster(observation, marioRow, marioCol, maxSearchCol, minSearchRow, maxSearchRow);
        if (cluster != null) return cluster;
        
        return null;
    }
    
    /**
     * Find horizontal coin trails
     */
    private CoinTrail findHorizontalCoinTrail(int[][] observation, int marioRow, int marioCol, int maxSearchCol) {
        for (int r = Math.max(0, marioRow - 1); r <= Math.min(observation.length - 1, marioRow + 1); r++) {
            int coinCount = 0;
            int startCol = -1;
            int endCol = -1;
            boolean hasPowerUp = false;
            
            for (int c = marioCol + 1; c < maxSearchCol; c++) {
                if (observation[r][c] == 2) { // Coin
                    if (startCol == -1) startCol = c;
                    endCol = c;
                    coinCount++;
                } else if (observation[r][c] == 9 || observation[r][c] == 7) { // Power-up
                    if (startCol == -1) startCol = c;
                    endCol = c;
                    coinCount++;
                    hasPowerUp = true;
                } else if (coinCount > 0) {
                    // Gap in trail, check if it's worth collecting
                    break;
                }
            }
            
            // Return trail if it's worth collecting (3+ coins or any power-up)
            if (coinCount >= 3 || hasPowerUp) {
                return new CoinTrail(r, startCol, r, endCol, coinCount, hasPowerUp || coinCount >= 5, "horizontal");
            }
        }
        return null;
    }
    
    /**
     * Find vertical coin trails (jumping sequences)
     */
    private CoinTrail findVerticalCoinTrail(int[][] observation, int marioRow, int marioCol, int maxSearchCol, int minSearchRow, int maxSearchRow) {
        for (int c = marioCol + 2; c < Math.min(maxSearchCol, marioCol + 6); c++) {
            int coinCount = 0;
            int startRow = -1;
            int endRow = -1;
            boolean hasPowerUp = false;
            
            for (int r = minSearchRow; r < maxSearchRow; r++) {
                if (observation[r][c] == 2) { // Coin
                    if (startRow == -1) startRow = r;
                    endRow = r;
                    coinCount++;
                } else if (observation[r][c] == 9 || observation[r][c] == 7) { // Power-up
                    if (startRow == -1) startRow = r;
                    endRow = r;
                    coinCount++;
                    hasPowerUp = true;
                }
            }
            
            // Vertical trails are valuable if they have 2+ coins or any power-up
            if (coinCount >= 2 || hasPowerUp) {
                return new CoinTrail(startRow, c, endRow, c, coinCount, hasPowerUp || coinCount >= 4, "vertical");
            }
        }
        return null;
    }
    
    /**
     * Find arc-shaped coin trails (common in Mario levels)
     */
    private CoinTrail findArcCoinTrail(int[][] observation, int marioRow, int marioCol, int maxSearchCol, int minSearchRow, int maxSearchRow) {
        // Look for arc patterns in a 4x4 area ahead
        for (int baseCol = marioCol + 2; baseCol < Math.min(maxSearchCol - 3, marioCol + 8); baseCol++) {
            int coinCount = 0;
            boolean hasPowerUp = false;
            int minRow = maxSearchRow, maxRow = minSearchRow;
            int minCol = maxSearchCol, maxCol = 0;
            
            // Check arc pattern
            for (int c = baseCol; c < baseCol + 4; c++) {
                for (int r = Math.max(minSearchRow, marioRow - 3); r <= Math.min(maxSearchRow - 1, marioRow + 1); r++) {
                    if (observation[r][c] == 2) { // Coin
                        coinCount++;
                        minRow = Math.min(minRow, r);
                        maxRow = Math.max(maxRow, r);
                        minCol = Math.min(minCol, c);
                        maxCol = Math.max(maxCol, c);
                    } else if (observation[r][c] == 9 || observation[r][c] == 7) { // Power-up
                        coinCount++;
                        hasPowerUp = true;
                        minRow = Math.min(minRow, r);
                        maxRow = Math.max(maxRow, r);
                        minCol = Math.min(minCol, c);
                        maxCol = Math.max(maxCol, c);
                    }
                }
            }
            
            // Arc trails are valuable if they span vertically and horizontally with good coin density
            if ((coinCount >= 4 || hasPowerUp) && (maxRow - minRow >= 2) && (maxCol - minCol >= 2)) {
                return new CoinTrail(minRow, minCol, maxRow, maxCol, coinCount, hasPowerUp || coinCount >= 6, "arc");
            }
        }
        return null;
    }
    
    /**
     * Find coin clusters worth detouring for
     */
    private CoinTrail findCoinCluster(int[][] observation, int marioRow, int marioCol, int maxSearchCol, int minSearchRow, int maxSearchRow) {
        // Look for dense clusters of coins in nearby areas
        for (int centerCol = marioCol + 2; centerCol < Math.min(maxSearchCol - 2, marioCol + 10); centerCol++) {
            for (int centerRow = Math.max(minSearchRow + 1, marioRow - 2); centerRow <= Math.min(maxSearchRow - 2, marioRow + 2); centerRow++) {
                int coinCount = 0;
                boolean hasPowerUp = false;
                
                // Check 3x3 area around center
                for (int r = centerRow - 1; r <= centerRow + 1; r++) {
                    for (int c = centerCol - 1; c <= centerCol + 1; c++) {
                        if (r >= 0 && r < observation.length && c >= 0 && c < observation[0].length) {
                            if (observation[r][c] == 2) { // Coin
                                coinCount++;
                            } else if (observation[r][c] == 9 || observation[r][c] == 7) { // Power-up
                                coinCount++;
                                hasPowerUp = true;
                            }
                        }
                    }
                }
                
                // Clusters are valuable if they have 4+ coins in small area or any power-up
                if (coinCount >= 4 || hasPowerUp) {
                    return new CoinTrail(centerRow - 1, centerCol - 1, centerRow + 1, centerCol + 1, coinCount, hasPowerUp || coinCount >= 6, "cluster");
                }
            }
        }
        return null;
    }
    
    /**
     * Adjust movement for predicted coin trail
     */
    private void adjustForCoinTrail(boolean[] actions, CoinTrail trail, int marioRow, int marioCol) {
        switch (trail.pattern) {
            case "horizontal":
                // Continue moving right at appropriate speed
                actions[MarioActions.RIGHT.getValue()] = true;
                actions[MarioActions.SPEED.getValue()] = !trail.isHighValue; // Slow down for high-value trails
                
                // Jump if coins are above current level
                if (trail.startRow < marioRow) {
                    actions[MarioActions.JUMP.getValue()] = true;
                }
                break;
                
            case "vertical":
                // Position for jumping sequence
                actions[MarioActions.RIGHT.getValue()] = true;
                actions[MarioActions.SPEED.getValue()] = false; // Precision movement
                
                // Jump for vertical collection
                if (Math.abs(marioCol + 1 - trail.startCol) <= 1) {
                    actions[MarioActions.JUMP.getValue()] = true;
                }
                break;
                
            case "arc":
                // Timing-based movement for arc collection
                actions[MarioActions.RIGHT.getValue()] = true;
                actions[MarioActions.SPEED.getValue()] = trail.coinCount >= 6; // Speed up for long arcs
                
                // Jump at optimal timing
                if (marioCol + 2 >= trail.startCol && marioCol <= trail.endCol - 2) {
                    actions[MarioActions.JUMP.getValue()] = true;
                }
                break;
                
            case "cluster":
                // Careful approach to cluster
                actions[MarioActions.RIGHT.getValue()] = true;
                actions[MarioActions.SPEED.getValue()] = false; // Slow and careful
                
                // Jump if cluster is elevated
                if (trail.startRow < marioRow - 1) {
                    actions[MarioActions.JUMP.getValue()] = true;
                }
                break;
        }
    }
}