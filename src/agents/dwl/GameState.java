package agents.dwl;

/**
 * Enumeration of possible game states for human-like Mario AI
 * Each state represents a different behavioral mode
 */
public enum GameState {
    EXPLORING("Cautious forward movement, scanning for threats and opportunities"),
    JUMPING("Obstacle avoidance and platform navigation");

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