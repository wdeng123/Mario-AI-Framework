package levelGenerators.dwl;

import engine.core.MarioLevelGenerator;
import engine.core.MarioLevelModel;
import engine.core.MarioTimer;
import java.util.Random;

public class LevelGenerator implements MarioLevelGenerator {
    private Random random;
    private MarkovChain markovChain;

    public LevelGenerator() {
        this.random = new Random();
        this.markovChain = new MarkovChain(this.random);
    }

    public LevelGenerator(long seed) {
        this.random = new Random(seed);
        this.markovChain = new MarkovChain(this.random);
    }

    @Override
    public String getGeneratedLevel(MarioLevelModel model, MarioTimer timer) {
        model.clearMap();
        markovChain.reset();

        // Calculate difficulty progression
        int levelLength = model.getWidth();
        int baseFloor = model.getHeight() - 2; // Leave one row for ground base

        // Generate level using Markov chain
        int currentX = 0;
        int chunkCount = 0;

        // Start area - always safe
        currentX += buildStartArea(model, currentX, baseFloor);

        // Generate main level content
        while (currentX < levelLength - 10 && timer.getRemainingTime() > 1000) {
            int difficulty = calculateDifficulty(currentX, levelLength);
            ChunkType nextChunkType = markovChain.generateNextChunk(difficulty, chunkCount, levelLength / 8);


            LevelChunk chunk = new LevelChunk(nextChunkType, baseFloor);
            int chunkWidth = chunk.generateChunk(model, currentX, random, difficulty);

            currentX += chunkWidth;
            chunkCount++;

            // Safety check to prevent infinite loops
            if (chunkCount > 50) {
                break;
            }
        }

        // End area - always safe and accessible
        buildEndArea(model, currentX, levelLength, baseFloor);

        return model.getMap();
    }

    private int buildStartArea(MarioLevelModel model, int startX, int baseFloor) {
        int length = 8;

        // Build safe starting ground
        for (int x = startX; x < startX + length && x < model.getWidth(); x++) {
            for (int y = baseFloor; y < model.getHeight(); y++) {
                model.setBlock(x, y, MarioLevelModel.GROUND);
            }
        }

        // Place Mario start position
        model.setBlock(1, baseFloor - 1, MarioLevelModel.MARIO_START);

        return length;
    }

    private void buildEndArea(MarioLevelModel model, int startX, int levelLength, int baseFloor) {
        // Fill remaining area with safe ground
        for (int x = startX; x < levelLength; x++) {
            for (int y = baseFloor; y < model.getHeight(); y++) {
                model.setBlock(x, y, MarioLevelModel.GROUND);
            }
        }

        // Place level exit
        int exitX = Math.max(levelLength - 3, startX + 2);
        if (exitX < levelLength - 1) {
            model.setBlock(exitX, baseFloor - 1, MarioLevelModel.PYRAMID_BLOCK);
            model.setBlock(exitX, baseFloor - 2, MarioLevelModel.MARIO_EXIT);
        }
    }

    private int calculateDifficulty(int currentX, int totalLength) {
        // Linear difficulty progression from 0 to 4
        double progress = (double) currentX / totalLength;
        return (int) Math.min(4, progress * 5);
    }

    @Override
    public String getGeneratorName() {
        return "MarkovChainLevelGenerator";
    }

    // Utility method for testing and analysis
    public MarkovChain getMarkovChain() {
        return markovChain;
    }
}