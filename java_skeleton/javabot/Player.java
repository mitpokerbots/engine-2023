package javabot;

import javabot.skeleton.Action;
import javabot.skeleton.ActionType;
import javabot.skeleton.GameState;
import javabot.skeleton.State;
import javabot.skeleton.TerminalState;
import javabot.skeleton.RoundState;
import javabot.skeleton.Bot;
import javabot.skeleton.Runner;

import java.util.List;
import java.util.Set;
import java.lang.Integer;
import java.lang.String;

/**
 * A pokerbot.
 */
public class Player implements Bot {
    // Your instance variables go here.

    /**
     * Called when a new game starts. Called exactly once.
     */
    public Player() {
    }

    /**
     * Called when a new round starts. Called State.NUM_ROUNDS times.
     *
     * @param gameState The GameState object.
     * @param roundState The RoundState object.
     * @param active Your player's index.
     */
    public void handleNewRound(GameState gameState, RoundState roundState, int active) {
        //int myBankroll = gameState.bankroll;  // the total number of chips you've gained or lost from the beginning of the game to the start of this round
        //float gameClock = gameState.gameClock;  // the total number of seconds your bot has left to play this game
        //int roundNum = gameState.roundNum;  // the round number from 1 to State.NUM_ROUNDS
        //List<String> myCards = roundState.hands.get(active);  // your cards
        //boolean bigBlind = (active == 1);  // true if you are the big blind
    }

    /**
     * Called when a round ends. Called State.NUM_ROUNDS times.
     *
     * @param gameState The GameState object.
     * @param terminalState The TerminalState object.
     * @param active Your player's index.
     */
    public void handleRoundOver(GameState gameState, TerminalState terminalState, int active) {
        //int myDelta = terminalState.deltas.get(active);  // your bankroll change from this round
        //RoundState previousState = (RoundState)(terminalState.previousState);  // RoundState before payoffs
        //int street = previousState.street;  // int of street representing when this round ended
        //List<String> myCards = previousState.hands.get(active);  // your cards
        //List<String> oppCards = previousState.hands.get(1-active);  // opponent's cards or "" if not revealed
    }

    /**
     * Where the magic happens - your code should implement this function.
     * Called any time the engine needs an action from your bot.
     *
     * @param gameState The GameState object.
     * @param roundState The RoundState object.
     * @param active Your player's index.
     * @return Your action.
     */
    public Action getAction(GameState gameState, RoundState roundState, int active) {
        Set<ActionType> legalActions = roundState.legalActions();  // the actions you are allowed to take
        //int street = roundState.street;  // int representing pre-flop, flop, turn, or river, etc. respectively
        //List<String> myCards = roundState.hands.get(active);  // your cards
        //List<String> boardCards = roundState.deck;  // the board cards
        //int myPip = roundState.pips.get(active);  // the number of chips you have contributed to the pot this round of betting
        //int oppPip = roundState.pips.get(1-active);  // the number of chips your opponent has contributed to the pot this round of betting
        //int myStack = roundState.stacks.get(active);  // the number of chips you have remaining
        //int oppStack = roundState.stacks.get(1-active);  // the number of chips your opponent has remaining
        //int continueCost = oppPip - myPip;  // the number of chips needed to stay in the pot
        //int myContribution = State.STARTING_STACK - myStack;  // the number of chips you have contributed to the pot
        //int oppContribution = State.STARTING_STACK - oppStack;  // the number of chips your opponent has contributed to the pot
        //if (legalActions.contains(ActionType.RAISE_ACTION_TYPE)) {
        //    List<Integer> raiseBounds = roundState.raiseBounds();  // the smallest and largest numbers of chips for a legal bet/raise
        //    int minCost = raiseBounds.get(0) - myPip;  // the cost of a minimum bet/raise
        //    int maxCost = raiseBounds.get(1) - myPip;  // the cost of a maximum bet/raise
        //}
        if (legalActions.contains(ActionType.CHECK_ACTION_TYPE)) {  // check-call
            return new Action(ActionType.CHECK_ACTION_TYPE);
        }
        return new Action(ActionType.CALL_ACTION_TYPE);
    }

    /**
     * Main program for running a Java pokerbot.
     */
    public static void main(String[] args) {
        Player player = new Player();
        Runner runner = new Runner();
        runner.parseArgs(args);
        runner.runBot(player);
    }
}
