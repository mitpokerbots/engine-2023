package javabot.skeleton;

/**
 * Stores higher state information across many rounds of poker.
 */
public class GameState {
    public final int bankroll;
    public final float gameClock;
    public final int roundNum;

    public GameState(int bankroll, float gameClock, int roundNum) {
        this.bankroll = bankroll;
        this.gameClock = gameClock;
        this.roundNum = roundNum;
    }
}