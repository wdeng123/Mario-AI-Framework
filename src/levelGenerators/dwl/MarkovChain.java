package levelGenerators.dwl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MarkovChain {
    private TransitionTable transitionTable;
    private ChunkType currentState;
    private List<ChunkType> generatedSequence;
    private Random random;

    public MarkovChain(Random random) {
        this.random = random;
        this.transitionTable = new TransitionTable();
        this.currentState = ChunkType.GROUND_FLAT; // Safe starting state
        this.generatedSequence = new ArrayList<>();
    }

    public ChunkType generateNextChunk(int difficulty, int currentPosition, int remainingLength) {
        // Apply constraints for safe level generation
        ChunkType nextChunk = applyConstraints(
            transitionTable.getNextChunk(currentState, random, difficulty),
            currentPosition,
            remainingLength
        );

        generatedSequence.add(nextChunk);
        currentState = nextChunk;

        return nextChunk;
    }

    private ChunkType applyConstraints(ChunkType proposed, int currentPosition, int remainingLength) {
        // Force safe end section when close to level end
        if (remainingLength < 20) {
            return ChunkType.GROUND_FLAT;
        }

        // Prevent consecutive challenging chunks
        if (generatedSequence.size() > 0) {
            ChunkType lastChunk = generatedSequence.get(generatedSequence.size() - 1);

            // No consecutive gaps
            if (isGapChunk(lastChunk) && isGapChunk(proposed)) {
                return ChunkType.GROUND_FLAT;
            }

            if (isGapChunk(lastChunk) && proposed == ChunkType.PLATFORM_JUMP) {
                return ChunkType.GROUND_FLAT;
            }

            // No consecutive enemy groups
            if (lastChunk == ChunkType.ENEMY_GROUP && proposed == ChunkType.ENEMY_GROUP) {
                return ChunkType.GROUND_FLAT;
            }

            // No consecutive platform jumps
            if (lastChunk == ChunkType.PLATFORM_JUMP && proposed == ChunkType.PLATFORM_JUMP) {
                return ChunkType.GROUND_FLAT;
            }
        }

        // Check for impossible sequences
        if (generatedSequence.size() >= 2) {
            ChunkType secondLast = generatedSequence.get(generatedSequence.size() - 2);
            ChunkType last = generatedSequence.get(generatedSequence.size() - 1);

            // Prevent gap -> platform -> gap (too difficult)
            if (isGapChunk(secondLast) && last == ChunkType.PLATFORM_JUMP && isGapChunk(proposed)) {
                return ChunkType.GROUND_FLAT;
            }
        }

        return proposed;
    }

    private boolean isGapChunk(ChunkType chunk) {
        return chunk == ChunkType.GAP_SMALL || chunk == ChunkType.GAP_LARGE;
    }

    public void reset() {
        currentState = ChunkType.GROUND_FLAT;
        generatedSequence.clear();
    }

    public ChunkType getCurrentState() {
        return currentState;
    }

    public List<ChunkType> getGeneratedSequence() {
        return new ArrayList<>(generatedSequence);
    }

    public TransitionTable getTransitionTable() {
        return transitionTable;
    }
}