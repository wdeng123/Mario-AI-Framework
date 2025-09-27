package levelGenerators.dwl;

import engine.core.MarioLevelModel;
import java.util.Random;

public class LevelChunk {
    private ChunkType type;
    private int width;
    private int baseFloor;

    public LevelChunk(ChunkType type, int baseFloor) {
        this.type = type;
        this.baseFloor = baseFloor;
        this.width = getDefaultWidth();
    }

    private int getDefaultWidth() {
        switch (type) {
            case GAP_SMALL: return 3;
            case GAP_LARGE: return 6;
            case PIPE_SECTION: return 10;
            case PLATFORM_JUMP: return 12;
            default: return 8;
        }
    }

    public int generateChunk(MarioLevelModel model, int startX, Random random, int difficulty) {
        switch (type) {
            case GROUND_FLAT:
                return buildGroundFlat(model, startX, random, difficulty);
            case GROUND_HILL:
                return buildGroundHill(model, startX, random, difficulty);
            case GAP_SMALL:
                return buildGapSmall(model, startX, random);
            case GAP_LARGE:
                return buildGapLarge(model, startX, random);
            case PIPE_SECTION:
                return buildPipeSection(model, startX, random, difficulty);
            case ENEMY_GROUP:
                return buildEnemyGroup(model, startX, random, difficulty);
            case COIN_COLLECTION:
                return buildCoinCollection(model, startX, random);
            case PLATFORM_JUMP:
                return buildPlatformJump(model, startX, random);
            default:
                return buildGroundFlat(model, startX, random, difficulty);
        }
    }

    private int buildGroundFlat(MarioLevelModel model, int startX, Random random, int difficulty) {
        int length = 8 + random.nextInt(4);

        for (int x = startX; x < startX + length && x < model.getWidth(); x++) {
            for (int y = baseFloor; y < model.getHeight(); y++) {
                model.setBlock(x, y, MarioLevelModel.GROUND);
            }
        }

        // Add enemies based on difficulty
        if (difficulty > 0 && random.nextInt(2) == 0) {
            int enemyX = startX + 2 + random.nextInt(Math.max(1, length - 4));
            if (random.nextInt(2) == 0) {
                model.setBlock(enemyX, baseFloor - 1, MarioLevelModel.GOOMBA);
            } else {
                model.setBlock(enemyX, baseFloor - 1, MarioLevelModel.GREEN_KOOPA);
            }
        }

        return length;
    }

    private int buildGroundHill(MarioLevelModel model, int startX, Random random, int difficulty) {
        int length = 10 + random.nextInt(4);

        // Base ground
        for (int x = startX; x < startX + length && x < model.getWidth(); x++) {
            for (int y = baseFloor; y < model.getHeight(); y++) {
                model.setBlock(x, y, MarioLevelModel.GROUND);
            }
        }

        // Add hill platform
        int hillStart = startX + 2;
        int hillLength = length - 4;
        int hillHeight = baseFloor - 2 - random.nextInt(2);

        for (int x = hillStart; x < hillStart + hillLength && x < model.getWidth(); x++) {
            for (int y = hillHeight; y < baseFloor; y++) {
                if (y == hillHeight) {
                    model.setBlock(x, y, MarioLevelModel.PLATFORM);
                } else {
                    model.setBlock(x, y, MarioLevelModel.PLATFORM_BACKGROUND);
                }
            }
        }

        return length;
    }

    private int buildGapSmall(MarioLevelModel model, int startX, Random random) {
        int gapWidth = 2 + random.nextInt(3); // 2-4 tiles
        return gapWidth;
    }

    private int buildGapLarge(MarioLevelModel model, int startX, Random random) {
        int gapWidth = 5 + random.nextInt(3); // 5-7 tiles
        return gapWidth;
    }

    private int buildPipeSection(MarioLevelModel model, int startX, Random random, int difficulty) {
        int length = 8 + random.nextInt(4);

        // Base ground
        for (int x = startX; x < startX + length && x < model.getWidth(); x++) {
            for (int y = baseFloor; y < model.getHeight(); y++) {
                model.setBlock(x, y, MarioLevelModel.GROUND);
            }
        }

        // Add pipe
        int pipeX = startX + 3 + random.nextInt(Math.max(1, length - 6));
        int pipeHeight = 2 + random.nextInt(3);
        char pipeType = (difficulty > 2 && random.nextInt(3) == 0) ?
            MarioLevelModel.PIPE_FLOWER : MarioLevelModel.PIPE;

        for (int x = pipeX; x < pipeX + 2 && x < model.getWidth(); x++) {
            for (int y = baseFloor - pipeHeight; y < baseFloor; y++) {
                model.setBlock(x, y, pipeType);
            }
        }

        return length;
    }

    private int buildEnemyGroup(MarioLevelModel model, int startX, Random random, int difficulty) {
        int length = 6 + random.nextInt(4);

        // Base ground
        for (int x = startX; x < startX + length && x < model.getWidth(); x++) {
            for (int y = baseFloor; y < model.getHeight(); y++) {
                model.setBlock(x, y, MarioLevelModel.GROUND);
            }
        }

        // Add enemy group
        char[] enemies = {MarioLevelModel.GOOMBA, MarioLevelModel.GREEN_KOOPA, MarioLevelModel.RED_KOOPA};
        for (int i = 1; i < length - 1; i += 2) {
            int x = startX + i;
            if (x < model.getWidth() && random.nextInt(2) == 0) {
                char enemy = enemies[random.nextInt(enemies.length)];
                model.setBlock(x, baseFloor - 1, enemy);
            }
        }

        return length;
    }

    private int buildCoinCollection(MarioLevelModel model, int startX, Random random) {
        int length = 8 + random.nextInt(4);

        // Base ground
        for (int x = startX; x < startX + length && x < model.getWidth(); x++) {
            for (int y = baseFloor; y < model.getHeight(); y++) {
                model.setBlock(x, y, MarioLevelModel.GROUND);
            }
        }

        // Add brick/coin arrangement
        int blockY = baseFloor - 4;
        for (int i = 2; i < length - 2; i++) {
            int x = startX + i;
            if (x < model.getWidth() && random.nextInt(3) != 0) {
                if (random.nextInt(3) == 0) {
                    model.setBlock(x, blockY, MarioLevelModel.COIN);
                } else if (random.nextInt(2) == 0) {
                    model.setBlock(x, blockY, MarioLevelModel.COIN_QUESTION_BLOCK);
                } else {
                    model.setBlock(x, blockY, MarioLevelModel.NORMAL_BRICK);
                }
            }
        }

        return length;
    }

    private int buildPlatformJump(MarioLevelModel model, int startX, Random random) {
        int length = 10 + random.nextInt(4);

        // Add landing ground first to ensure reachability after gaps
        for (int x = startX; x < startX + 3 && x < model.getWidth(); x++) {
            for (int y = baseFloor; y < model.getHeight(); y++) {
                model.setBlock(x, y, MarioLevelModel.GROUND);
            }
        }

        // Create floating platforms
        int platformY = baseFloor - 3 - random.nextInt(2);
        int platformStart = startX + 3;
        int platformLength = 3;

        for (int x = platformStart; x < platformStart + platformLength && x < model.getWidth(); x++) {
            model.setBlock(x, platformY, MarioLevelModel.PLATFORM);
        }

        // Add second platform if space allows
        if (length > 8) {
            int secondPlatformX = platformStart + platformLength + 3;
            if (secondPlatformX + 2 < startX + length && secondPlatformX + 2 < model.getWidth()) {
                for (int x = secondPlatformX; x < secondPlatformX + 2; x++) {
                    model.setBlock(x, platformY, MarioLevelModel.PLATFORM);
                }
            }
        }

        return length;
    }

    public ChunkType getType() {
        return type;
    }

    public int getWidth() {
        return width;
    }
}