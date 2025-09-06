package agents.dwl;

/**
 * Enumeration of possible game states for human-like Mario AI
 * Each state represents a different behavioral mode
 * Phase 3: Enhanced with additional human-like states
 */
public enum GameState {
    EXPLORING("Cautious forward movement, scanning for threats and opportunities"),
    JUMPING("Obstacle avoidance and platform navigation"),
    COLLECTING("Focused collection of coins and power-ups"),
    FLEEING("Escape from overwhelming enemy threats"),
    STUCK("Repeated attempts to overcome obstacles"),
    HESITATING("Careful assessment before risky moves");

    private final String description;

    GameState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name() + ": " + description;
    }
}