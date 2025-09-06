package agents.dwl;

import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.MarioActions;

/**
 * Fleeing State - Escape from overwhelming enemy threats
 * Human-like panic response to dangerous situations
 */
public class FleeingState implements FSMState {
    private int fleeingTimer = 0;
    private boolean hasEscapedThreat = false;
    
    @Override
    public boolean[] getActions(MarioForwardModel model, Agent agent, MarioTimer timer) {
        boolean[] actions = new boolean[MarioActions.numberOfActions()];
        
        int[][] enemies = model.getMarioEnemiesObservation();
        int marioRow = enemies.length / 2;
        int marioCol = enemies[0].length / 2;
        
        // Human-like fleeing behavior - often suboptimal but realistic
        if (fleeingTimer < 30) {
            // Initial panic phase - erratic movement
            if (Math.random() < 0.7) {
                actions[MarioActions.RIGHT.getValue()] = true; // Usually run forward
                actions[MarioActions.SPEED.getValue()] = true; // Run fast
            } else {
                // Sometimes freeze in panic
                actions[MarioActions.RIGHT.getValue()] = false;
            }
            
            // Panic jumping
            if (Math.random() < 0.4) {
                actions[MarioActions.JUMP.getValue()] = true;
            }
        } else {
            // More controlled escape after initial panic
            boolean threatAhead = hasEnemyAhead(enemies, marioRow, marioCol);
            
            if (threatAhead) {
                // Jump over enemies when possible
                actions[MarioActions.JUMP.getValue()] = true;
                actions[MarioActions.RIGHT.getValue()] = true;
                actions[MarioActions.SPEED.getValue()] = true;
            } else {
                // Fast forward movement to escape area
                actions[MarioActions.RIGHT.getValue()] = true;
                actions[MarioActions.SPEED.getValue()] = true;
                
                // Occasional nervous jumping
                if (Math.random() < 0.2) {
                    actions[MarioActions.JUMP.getValue()] = true;
                }
            }
        }
        
        // Human mistake during panic - sometimes wrong inputs
        if (agent.getEmotionSystem().shouldMakeMistake() && Math.random() < 0.3) {
            // Panic-induced input errors
            if (Math.random() < 0.5) {
                actions[MarioActions.RIGHT.getValue()] = false; // Stop running
            } else {
                actions[MarioActions.SPEED.getValue()] = false; // Forget to run
            }
        }
        
        fleeingTimer++;
        
        // Check if we've escaped the immediate threat
        if (!hasImmediateEnemyThreat(enemies, marioRow, marioCol)) {
            hasEscapedThreat = true;
        }
        
        return actions;
    }
    
    @Override
    public GameState checkTransitions(MarioForwardModel model, Agent agent) {
        int[][] observation = model.getMarioSceneObservation();
        int[][] enemies = model.getMarioEnemiesObservation();
        int marioRow = observation.length / 2;
        int marioCol = observation[0].length / 2;
        
        // Transition to JUMPING if obstacle blocks escape route
        if (hasBlockingObstacle(observation, marioRow, marioCol)) {
            return GameState.JUMPING;
        }
        
        // Return to EXPLORING once threat is escaped and we've fled long enough
        if (hasEscapedThreat && fleeingTimer > 60 && 
            !hasImmediateEnemyThreat(enemies, marioRow, marioCol)) {
            return GameState.EXPLORING;
        }
        
        // If stuck fleeing for too long, try different approach
        if (fleeingTimer > 200) {
            if (agent.getStuckCounter() > 0) {
                return GameState.STUCK;
            } else {
                return GameState.JUMPING; // Try to jump over threats
            }
        }
        
        return null; // Stay in FLEEING
    }
    
    @Override
    public void onEnter(MarioForwardModel model, Agent agent) {
        fleeingTimer = 0;
        hasEscapedThreat = false;
        
        // Record that we're in a threatening situation (affects emotions)
        agent.getEmotionSystem().recordFailedJump(); // Treat as failure that led to fleeing
    }
    
    @Override
    public GameState getGameState() {
        return GameState.FLEEING;
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
     * Check if there are enemies ahead in escape path
     */
    private boolean hasEnemyAhead(int[][] enemies, int marioRow, int marioCol) {
        for (int c = marioCol + 1; c < Math.min(enemies[0].length, marioCol + 4); c++) {
            for (int r = Math.max(0, marioRow - 1); r <= Math.min(enemies.length - 1, marioRow + 1); r++) {
                if (enemies[r][c] != 0) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Check if obstacle is blocking escape route
     */
    private boolean hasBlockingObstacle(int[][] observation, int marioRow, int marioCol) {
        if (marioCol + 1 < observation[0].length) {
            // Check for wall blocking forward movement
            int wallHeight = 0;
            for (int r = marioRow; r >= 0; r--) {
                if (observation[r][marioCol + 1] != 0) {
                    wallHeight++;
                } else {
                    break;
                }
            }
            
            // If wall is higher than Mario can easily jump, it's blocking
            return wallHeight > 3;
        }
        return false;
    }
}