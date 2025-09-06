package agents.dwl;

import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.MarioActions;

/**
 * Jumping State - Handles complex obstacle navigation and platform sequences
 * Includes human-like jumping behavior with occasional timing mistakes
 */
public class JumpingState implements FSMState {
    private int jumpTimer = 0;
    private boolean commitedToJump = false;
    
    @Override
    public boolean[] getActions(MarioForwardModel model, Agent agent, MarioTimer timer) {
        boolean[] actions = new boolean[MarioActions.numberOfActions()];
        
        int[][] observation = model.getMarioSceneObservation();
        int marioRow = observation.length / 2;
        int marioCol = observation[0].length / 2;
        
        // Continue moving right unless there's a reason not to
        actions[MarioActions.RIGHT.getValue()] = true;
        actions[MarioActions.SPEED.getValue()] = true; // Run for better jumps
        
        // Handle jump timing
        if (commitedToJump || shouldStartJump(observation, marioRow, marioCol)) {
            if (!commitedToJump) {
                commitedToJump = true;
                jumpTimer = getJumpDuration(observation, marioRow, marioCol);
                
                // Human-like mistake: occasionally misjudge jump timing
                if (agent.getEmotionSystem().shouldMakeMistake()) {
                    jumpTimer += (int)((Math.random() - 0.5) * 10); // ±5 frame timing error
                }
            }
            
            if (jumpTimer > 0) {
                actions[MarioActions.JUMP.getValue()] = true;
                jumpTimer--;
            } else {
                commitedToJump = false;
            }
        }
        
        // Adjust horizontal movement for landing
        adjustHorizontalMovement(actions, observation, marioRow, marioCol, model);
        
        return actions;
    }
    
    @Override
    public GameState checkTransitions(MarioForwardModel model, Agent agent) {
        int[][] observation = model.getMarioSceneObservation();
        int marioRow = observation.length / 2;
        int marioCol = observation[0].length / 2;
        
        // Return to EXPLORING if jump sequence complete (Phase 2: Simple logic)
        if (!commitedToJump && !needsContinuousJumping(observation, marioRow, marioCol)) {
            return GameState.EXPLORING;
        }
        
        return null; // Stay in JUMPING
    }
    
    @Override
    public void onEnter(MarioForwardModel model, Agent agent) {
        jumpTimer = 0;
        commitedToJump = false;
    }
    
    @Override
    public GameState getGameState() {
        return GameState.JUMPING;
    }
    
    /**
     * Determine if Mario should start a jump
     */
    private boolean shouldStartJump(int[][] observation, int marioRow, int marioCol) {
        // Check for obstacles that require jumping
        if (marioCol + 1 < observation[0].length) {
            // Immediate obstacle
            if (observation[marioRow][marioCol + 1] != 0 || 
                observation[marioRow + 1][marioCol + 1] != 0) {
                return true;
            }
            
            // Gap ahead
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
     * Calculate appropriate jump duration based on obstacle
     */
    private int getJumpDuration(int[][] observation, int marioRow, int marioCol) {
        // Base jump duration
        int duration = 8;
        
        // Longer jump for wider gaps
        int gapWidth = measureGapWidth(observation, marioRow, marioCol);
        if (gapWidth > 0) {
            duration = Math.max(8, gapWidth * 2);
        }
        
        // Longer jump for high obstacles
        int obstacleHeight = measureObstacleHeight(observation, marioRow, marioCol);
        if (obstacleHeight > 0) {
            duration = Math.max(duration, obstacleHeight * 3);
        }
        
        // Human-like variation: slightly inconsistent timing
        duration += (int)(Math.random() * 4 - 2); // ±2 frame variation
        
        return Math.max(5, Math.min(20, duration)); // Clamp to reasonable range
    }
    
    /**
     * Check for gaps ahead of Mario
     */
    private boolean hasGapAhead(int[][] observation, int marioRow, int marioCol) {
        if (marioCol + 2 >= observation[0].length) return false;
        
        // Check if ground disappears
        for (int c = marioCol + 1; c < Math.min(observation[0].length, marioCol + 4); c++) {
            boolean hasGround = false;
            for (int r = marioRow + 1; r < observation.length; r++) {
                if (observation[r][c] != 0) {
                    hasGround = true;
                    break;
                }
            }
            if (!hasGround) {
                return true; // Gap found
            }
        }
        return false;
    }
    
    /**
     * Measure width of gap ahead
     */
    private int measureGapWidth(int[][] observation, int marioRow, int marioCol) {
        int gapWidth = 0;
        
        for (int c = marioCol + 1; c < observation[0].length; c++) {
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
     * Adjust horizontal movement for better landing
     */
    private void adjustHorizontalMovement(boolean[] actions, int[][] observation, 
                                        int marioRow, int marioCol, MarioForwardModel model) {
        // Human-like behavior: sometimes overcorrect or undercorrect
        if (model.isMarioOnGround()) {
            // On ground - normal movement
            actions[MarioActions.RIGHT.getValue()] = true;
        } else {
            // In air - adjust for landing
            if (Math.random() < 0.1) {
                // Occasional overcorrection (human-like)
                actions[MarioActions.RIGHT.getValue()] = Math.random() < 0.7;
            }
        }
    }
    
    /**
     * Check if continuous jumping is needed
     */
    private boolean needsContinuousJumping(int[][] observation, int marioRow, int marioCol) {
        // Check for series of obstacles or platforms requiring multiple jumps
        int obstacleCount = 0;
        
        for (int c = marioCol + 1; c < Math.min(observation[0].length, marioCol + 6); c++) {
            if (observation[marioRow][c] != 0 || 
                (marioRow + 1 < observation.length && observation[marioRow + 1][c] != 0)) {
                obstacleCount++;
            }
        }
        
        return obstacleCount > 2; // Multiple obstacles = keep jumping
    }
    
    /**
     * Check for enemy threats during jumping
     */
    private boolean hasEnemyThreat(int[][] enemies, int marioRow, int marioCol) {
        // Scan area around Mario for enemies
        for (int r = marioRow - 1; r <= marioRow + 1; r++) {
            for (int c = marioCol - 1; c <= marioCol + 3; c++) {
                if (r >= 0 && r < enemies.length && c >= 0 && c < enemies[0].length) {
                    if (enemies[r][c] != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Check for valuable items in reach during jump
     */
    private boolean hasValuableItemInReach(int[][] observation, int marioRow, int marioCol) {
        // Check for power-up blocks or multiple coins reachable by jumping
        for (int r = Math.max(0, marioRow - 3); r <= marioRow; r++) {
            for (int c = marioCol; c < Math.min(observation[0].length, marioCol + 3); c++) {
                if (observation[r][c] == 9) { // Power-up block
                    return true;
                }
            }
        }
        return false;
    }
}