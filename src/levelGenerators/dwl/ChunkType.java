package levelGenerators.dwl;

public enum ChunkType {
    GROUND_FLAT,    // Basic ground platform
    GROUND_HILL,    // Elevated ground with platforms
    GAP_SMALL,      // 2-4 tile jump gap
    GAP_LARGE,      // 5-7 tile running jump gap
    PIPE_SECTION,   // Pipe obstacles
    ENEMY_GROUP,    // Dense enemy placement
    COIN_COLLECTION, // Brick/question block arrangements
    PLATFORM_JUMP   // Floating platform sequences
}