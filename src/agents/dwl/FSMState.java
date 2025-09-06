package agents.dwl;

import engine.core.MarioForwardModel;
import engine.core.MarioTimer;

/**
 * Interface for Finite State Machine states
 * Each state implements specific behavior and transition logic
 */
public interface FSMState {
    /**
     * Execute the behavior for this state
     * @param model The forward model for game state analysis
     * @param agent Reference to the main agent for accessing emotion system
     * @param timer Timer for tracking execution time
     * @return Array of actions to perform
     */
    boolean[] getActions(MarioForwardModel model, Agent agent, MarioTimer timer);
    
    /**
     * Determine if this state should transition to another state
     * @param model The forward model for game state analysis
     * @param agent Reference to the main agent
     * @return The next state to transition to, or null to remain in current state
     */
    GameState checkTransitions(MarioForwardModel model, Agent agent);
    
    /**
     * Called when entering this state
     * @param model The current game state
     * @param agent Reference to the main agent
     */
    default void onEnter(MarioForwardModel model, Agent agent) {
        // Default implementation does nothing
    }
    
    /**
     * Called when leaving this state  
     * @param model The current game state
     * @param agent Reference to the main agent
     */
    default void onExit(MarioForwardModel model, Agent agent) {
        // Default implementation does nothing
    }
    
    /**
     * Get the game state this FSM state represents
     * @return The corresponding GameState enum
     */
    GameState getGameState();
}