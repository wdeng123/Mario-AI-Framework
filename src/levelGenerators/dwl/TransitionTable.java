package levelGenerators.dwl;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TransitionTable {
    private Map<ChunkType, Map<ChunkType, Double>> transitions;

    public TransitionTable() {
        initializeTransitions();
    }

    private void initializeTransitions() {
        transitions = new HashMap<>();

        // GROUND_FLAT transitions - safe, balanced
        Map<ChunkType, Double> groundFlatTrans = new HashMap<>();
        groundFlatTrans.put(ChunkType.GROUND_FLAT, 0.3);
        groundFlatTrans.put(ChunkType.GROUND_HILL, 0.2);
        groundFlatTrans.put(ChunkType.GAP_SMALL, 0.15);
        groundFlatTrans.put(ChunkType.PIPE_SECTION, 0.15);
        groundFlatTrans.put(ChunkType.COIN_COLLECTION, 0.15);
        groundFlatTrans.put(ChunkType.ENEMY_GROUP, 0.1);
        groundFlatTrans.put(ChunkType.GAP_LARGE, 0.0);
        groundFlatTrans.put(ChunkType.PLATFORM_JUMP, 0.05);
        transitions.put(ChunkType.GROUND_FLAT, groundFlatTrans);

        // GROUND_HILL transitions - recovery after hills
        Map<ChunkType, Double> groundHillTrans = new HashMap<>();
        groundHillTrans.put(ChunkType.GROUND_FLAT, 0.5);
        groundHillTrans.put(ChunkType.GROUND_HILL, 0.1);
        groundHillTrans.put(ChunkType.GAP_SMALL, 0.2);
        groundHillTrans.put(ChunkType.PIPE_SECTION, 0.1);
        groundHillTrans.put(ChunkType.COIN_COLLECTION, 0.05);
        groundHillTrans.put(ChunkType.ENEMY_GROUP, 0.05);
        groundHillTrans.put(ChunkType.GAP_LARGE, 0.0);
        groundHillTrans.put(ChunkType.PLATFORM_JUMP, 0.0);
        transitions.put(ChunkType.GROUND_HILL, groundHillTrans);

        // GAP_SMALL transitions - recovery required
        Map<ChunkType, Double> gapSmallTrans = new HashMap<>();
        gapSmallTrans.put(ChunkType.GROUND_FLAT, 0.6);
        gapSmallTrans.put(ChunkType.GROUND_HILL, 0.2);
        gapSmallTrans.put(ChunkType.GAP_SMALL, 0.0);
        gapSmallTrans.put(ChunkType.PIPE_SECTION, 0.1);
        gapSmallTrans.put(ChunkType.COIN_COLLECTION, 0.05);
        gapSmallTrans.put(ChunkType.ENEMY_GROUP, 0.05);
        gapSmallTrans.put(ChunkType.GAP_LARGE, 0.0);
        gapSmallTrans.put(ChunkType.PLATFORM_JUMP, 0.0);
        transitions.put(ChunkType.GAP_SMALL, gapSmallTrans);

        // GAP_LARGE transitions - mandatory recovery
        Map<ChunkType, Double> gapLargeTrans = new HashMap<>();
        gapLargeTrans.put(ChunkType.GROUND_FLAT, 0.8);
        gapLargeTrans.put(ChunkType.GROUND_HILL, 0.2);
        gapLargeTrans.put(ChunkType.GAP_SMALL, 0.0);
        gapLargeTrans.put(ChunkType.PIPE_SECTION, 0.0);
        gapLargeTrans.put(ChunkType.COIN_COLLECTION, 0.0);
        gapLargeTrans.put(ChunkType.ENEMY_GROUP, 0.0);
        gapLargeTrans.put(ChunkType.GAP_LARGE, 0.0);
        gapLargeTrans.put(ChunkType.PLATFORM_JUMP, 0.0);
        transitions.put(ChunkType.GAP_LARGE, gapLargeTrans);

        // PIPE_SECTION transitions - moderate variety
        Map<ChunkType, Double> pipeTrans = new HashMap<>();
        pipeTrans.put(ChunkType.GROUND_FLAT, 0.4);
        pipeTrans.put(ChunkType.GROUND_HILL, 0.1);
        pipeTrans.put(ChunkType.GAP_SMALL, 0.2);
        pipeTrans.put(ChunkType.PIPE_SECTION, 0.1);
        pipeTrans.put(ChunkType.COIN_COLLECTION, 0.1);
        pipeTrans.put(ChunkType.ENEMY_GROUP, 0.05);
        pipeTrans.put(ChunkType.GAP_LARGE, 0.0);
        pipeTrans.put(ChunkType.PLATFORM_JUMP, 0.05);
        transitions.put(ChunkType.PIPE_SECTION, pipeTrans);

        // ENEMY_GROUP transitions - recovery after combat
        Map<ChunkType, Double> enemyTrans = new HashMap<>();
        enemyTrans.put(ChunkType.GROUND_FLAT, 0.6);
        enemyTrans.put(ChunkType.GROUND_HILL, 0.1);
        enemyTrans.put(ChunkType.GAP_SMALL, 0.1);
        enemyTrans.put(ChunkType.PIPE_SECTION, 0.1);
        enemyTrans.put(ChunkType.COIN_COLLECTION, 0.1);
        enemyTrans.put(ChunkType.ENEMY_GROUP, 0.0);
        enemyTrans.put(ChunkType.GAP_LARGE, 0.0);
        enemyTrans.put(ChunkType.PLATFORM_JUMP, 0.0);
        transitions.put(ChunkType.ENEMY_GROUP, enemyTrans);

        // COIN_COLLECTION transitions - reward followed by variety
        Map<ChunkType, Double> coinTrans = new HashMap<>();
        coinTrans.put(ChunkType.GROUND_FLAT, 0.4);
        coinTrans.put(ChunkType.GROUND_HILL, 0.15);
        coinTrans.put(ChunkType.GAP_SMALL, 0.15);
        coinTrans.put(ChunkType.PIPE_SECTION, 0.1);
        coinTrans.put(ChunkType.COIN_COLLECTION, 0.05);
        coinTrans.put(ChunkType.ENEMY_GROUP, 0.1);
        coinTrans.put(ChunkType.GAP_LARGE, 0.0);
        coinTrans.put(ChunkType.PLATFORM_JUMP, 0.05);
        transitions.put(ChunkType.COIN_COLLECTION, coinTrans);

        // PLATFORM_JUMP transitions - recovery after platforming
        Map<ChunkType, Double> platformTrans = new HashMap<>();
        platformTrans.put(ChunkType.GROUND_FLAT, 0.7);
        platformTrans.put(ChunkType.GROUND_HILL, 0.2);
        platformTrans.put(ChunkType.GAP_SMALL, 0.1);
        platformTrans.put(ChunkType.PIPE_SECTION, 0.0);
        platformTrans.put(ChunkType.COIN_COLLECTION, 0.0);
        platformTrans.put(ChunkType.ENEMY_GROUP, 0.0);
        platformTrans.put(ChunkType.GAP_LARGE, 0.0);
        platformTrans.put(ChunkType.PLATFORM_JUMP, 0.0);
        transitions.put(ChunkType.PLATFORM_JUMP, platformTrans);
    }

    public ChunkType getNextChunk(ChunkType current, Random random, int difficulty) {
        Map<ChunkType, Double> probs = transitions.get(current);
        if (probs == null) {
            return ChunkType.GROUND_FLAT; // Safe fallback
        }

        // Apply difficulty scaling - increase challenge probability
        Map<ChunkType, Double> scaledProbs = applyDifficultyScaling(probs, difficulty);

        // Weighted random selection
        double total = scaledProbs.values().stream().mapToDouble(Double::doubleValue).sum();
        double rand = random.nextDouble() * total;
        double cumulative = 0.0;

        for (Map.Entry<ChunkType, Double> entry : scaledProbs.entrySet()) {
            cumulative += entry.getValue();
            if (rand <= cumulative) {
                return entry.getKey();
            }
        }

        return ChunkType.GROUND_FLAT; // Safe fallback
    }

    private Map<ChunkType, Double> applyDifficultyScaling(Map<ChunkType, Double> baseProbabilities, int difficulty) {
        Map<ChunkType, Double> scaled = new HashMap<>(baseProbabilities);

        // Linear progression: slightly increase challenge types as difficulty increases
        double difficultyFactor = 1.0 + (difficulty * 0.1); // 10% increase per difficulty level

        // Boost challenging chunk probabilities
        if (scaled.containsKey(ChunkType.GAP_LARGE)) {
            scaled.put(ChunkType.GAP_LARGE,
                Math.min(0.1, scaled.get(ChunkType.GAP_LARGE) * difficultyFactor));
        }
        if (scaled.containsKey(ChunkType.ENEMY_GROUP)) {
            scaled.put(ChunkType.ENEMY_GROUP,
                Math.min(0.15, scaled.get(ChunkType.ENEMY_GROUP) * difficultyFactor));
        }
        if (scaled.containsKey(ChunkType.PLATFORM_JUMP)) {
            scaled.put(ChunkType.PLATFORM_JUMP,
                Math.min(0.1, scaled.get(ChunkType.PLATFORM_JUMP) * difficultyFactor));
        }

        // Slightly reduce safe chunk probabilities to maintain balance
        double reduction = Math.min(0.1, (difficulty * 0.02));
        if (scaled.containsKey(ChunkType.GROUND_FLAT)) {
            scaled.put(ChunkType.GROUND_FLAT,
                Math.max(0.2, scaled.get(ChunkType.GROUND_FLAT) - reduction));
        }

        return scaled;
    }

    public Map<ChunkType, Map<ChunkType, Double>> getAllTransitions() {
        return new HashMap<>(transitions);
    }
}