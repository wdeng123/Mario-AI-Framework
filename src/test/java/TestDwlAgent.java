import engine.core.MarioGame;
import engine.core.MarioResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Test class for Phase 2 - Basic FSM functionality of dwl agent
 */
public class TestDwlAgent {
    public static void printResults(MarioResult result) {
        System.out.println("****************************************************************");
        System.out.println("Game Status: " + result.getGameStatus().toString() +
                " Percentage Completion: " + result.getCompletionPercentage());
        System.out.println("Lives: " + result.getCurrentLives() + " Coins: " + result.getCurrentCoins() +
                " Remaining Time: " + (int) Math.ceil(result.getRemainingTime() / 1000f));
        System.out.println("Mario State: " + result.getMarioMode() +
                " (Mushrooms: " + result.getNumCollectedMushrooms() + " Fire Flowers: " + result.getNumCollectedFireflower() + ")");
        System.out.println("Total Kills: " + result.getKillsTotal() + " (Stomps: " + result.getKillsByStomp() +
                " Fireballs: " + result.getKillsByFire() + " Shells: " + result.getKillsByShell() +
                " Falls: " + result.getKillsByFall() + ")");
        System.out.println("Bricks: " + result.getNumDestroyedBricks() + " Jumps: " + result.getNumJumps() +
                " Max X Jump: " + result.getMaxXJump() + " Max Air Time: " + result.getMaxJumpAirTime());
        System.out.println("****************************************************************");
    }

    public static String getLevel(String filepath) {
        String content = "";
        try {
            content = new String(Files.readAllBytes(Paths.get(filepath)));
        } catch (IOException e) {
            System.err.println("Error loading level: " + e.getMessage());
        }
        return content;
    }

    public static void main(String[] args) {
        MarioGame game = new MarioGame();
        
        System.out.println("Testing Phase 2 - dwl Human-like Mario Agent with FSM");
        System.out.println("States: EXPLORING <-> JUMPING");
        
        try {
            // Test with dwl agent on first Mario level
            printResults(game.runGame(new agents.dwl.Agent(), getLevel("./levels/original/lvl-1.txt"), 20, 0, true));
        } catch (Exception e) {
            System.err.println("Error running dwl agent: " + e.getMessage());
            e.printStackTrace();
        }
    }
}