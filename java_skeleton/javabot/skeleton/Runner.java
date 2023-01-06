package javabot.skeleton;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.lang.Integer;
import java.lang.String;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Interacts with the engine.
 */
public class Runner {
    private String host;
    private int port;
    private Bot pokerbot;
    private Socket socket;
    private PrintWriter outStream;
    private BufferedReader inStream;

    /**
     * Returns an incoming message from the engine.
     */
    public String[] receive() throws IOException {
        String line = this.inStream.readLine().trim();
        return line.split(" ");
    }

    /**
     * Encodes an action and sends it to the engine.
     */
    public void send(Action action) {
        String code;
        switch (action.actionType) {
            case FOLD_ACTION_TYPE: {
                code = "F";
                break;
            }
            case CALL_ACTION_TYPE: {
                code = "C";
                break;
            }
            case CHECK_ACTION_TYPE: {
                code = "K";
                break;
            }
            default: {  // RAISE_ACTION_TYPE
                code = "R" + Integer.toString(action.amount);
                break;
            }
        }
        this.outStream.println(code);
    }

    /**
     * Reconstructs the game tree based on the action history received from the engine.
     */
    public void run() throws IOException {
        GameState gameState = new GameState(0, (float)0., 1);
        State roundState = new RoundState(0, 0, Arrays.asList(0, 0), Arrays.asList(0, 0),
                                          Arrays.asList(Arrays.asList(""), Arrays.asList("")),
                                          Arrays.asList(""), null);
        int active = 0;
        boolean roundFlag = true;
        while (true) {
            String[] packet = this.receive();
            for (String clause : packet) {
                String leftover = clause.substring(1, clause.length());
                switch (clause.charAt(0)) {
                    case 'T': {
                        gameState = new GameState(gameState.bankroll, Float.parseFloat(leftover), gameState.roundNum);
                        break;
                    }
                    case 'P': {
                        active = Integer.parseInt(leftover);
                        break;
                    }
                    case 'H': {
                        String[] cards = leftover.split(",");
                        List<List<String>> hands = new ArrayList<List<String>>(
                            Arrays.asList(
                                new ArrayList<String>(),
                                new ArrayList<String>()
                            )
                        );
                        hands.set(active, Arrays.asList(cards[0], cards[1]));
                        hands.set(1 - active, Arrays.asList("", ""));
                        List<String> deck = new ArrayList<String>(Arrays.asList("", "", "", "", ""));
                        List<Integer> pips = Arrays.asList(State.SMALL_BLIND, State.BIG_BLIND);
                        List<Integer> stacks = Arrays.asList(State.STARTING_STACK - State.SMALL_BLIND,
                                                             State.STARTING_STACK - State.BIG_BLIND);
                        roundState = new RoundState(0, 0, pips, stacks, hands, deck, null);
                        if (roundFlag) {
                            this.pokerbot.handleNewRound(gameState, (RoundState)roundState, active);
                            roundFlag = false;
                        }
                        break;
                    }
                    case 'F': {
                        roundState = ((RoundState)roundState).proceed(new Action(ActionType.FOLD_ACTION_TYPE));
                        break;
                    }
                    case 'C': {
                        roundState = ((RoundState)roundState).proceed(new Action(ActionType.CALL_ACTION_TYPE));
                        break;
                    }
                    case 'K': {
                        roundState = ((RoundState)roundState).proceed(new Action(ActionType.CHECK_ACTION_TYPE));
                        break;
                    }
                    case 'R': {
                        roundState = ((RoundState)roundState).proceed(new Action(ActionType.RAISE_ACTION_TYPE,
                                                                                 Integer.parseInt(leftover)));
                        break;
                    }
                    case 'B': {
                        String[] cards = leftover.split(",");
                        List<String> revisedDeck = new ArrayList<String>();
                        for (int i = 0; i < cards.length; i++) {
                            revisedDeck.add("");
                            revisedDeck.set(i, cards[i]);
                        }
                        RoundState maker = (RoundState)roundState;
                        roundState = new RoundState(maker.button, maker.street, maker.pips, maker.stacks,
                                                    maker.hands, revisedDeck, maker.previousState);
                        break;
                    }
                    case 'O': {
                        // backtrack
                        String[] cards = leftover.split(",");
                        roundState = ((RoundState)roundState).previousState;
                        RoundState maker = (RoundState)roundState;
                        List<List<String>> revisedHands = new ArrayList<List<String>>(maker.hands);
                        revisedHands.set(1 - active, Arrays.asList(cards[0], cards[1]));
                        // rebuild history
                        roundState = new RoundState(maker.button, maker.street, maker.pips, maker.stacks,
                                                    revisedHands, maker.deck, maker.previousState);
                        roundState = new TerminalState(Arrays.asList(0, 0), roundState);
                        break;
                    }
                    case 'D': {
                        int delta = Integer.parseInt(leftover);
                        List<Integer> deltas = new ArrayList<Integer>(Arrays.asList(-1 * delta, -1 * delta));
                        deltas.set(active, delta);
                        roundState = new TerminalState(deltas, ((TerminalState)roundState).previousState);
                        gameState = new GameState(gameState.bankroll + delta, gameState.gameClock, gameState.roundNum);
                        this.pokerbot.handleRoundOver(gameState, (TerminalState)roundState, active);
                        gameState = new GameState(gameState.bankroll, gameState.gameClock, gameState.roundNum + 1);
                        roundFlag = true;
                        break;
                    }
                    case 'Q': {
                        return;
                    }
                    default: {
                        break;
                    }
                }
            }
            if (roundFlag) {  // ack the engine
                this.send(new Action(ActionType.CHECK_ACTION_TYPE));
            } else {
                Action action = this.pokerbot.getAction(gameState, (RoundState)roundState, active);
                this.send(action);
            }
        }
    }

    /**
     * Parses arguments corresponding to socket connection information.
     */
    public void parseArgs(String[] rawArgs) {
        boolean hostFlag = false;
        this.host = "localhost";
        for (String arg : rawArgs) {
            if (arg.equals("-h") | arg.equals("--host")) {
                hostFlag = true;
            } else if (arg.equals("--port")) {
                // nothing to do
            } else if (hostFlag) {
                this.host = arg;
                hostFlag = false;
            } else {
                this.port = Integer.parseInt(arg);
            }
        }
    }

    /**
     * Runs the pokerbot.
     */
    public void runBot(Bot pokerbot) {
        this.pokerbot = pokerbot;
        try {
            this.socket = new Socket(this.host, this.port);
            this.socket.setTcpNoDelay(true);
            this.outStream = new PrintWriter(socket.getOutputStream(), true);
            this.inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println("Could not connect to " + host + ":" + Integer.toString(port));
            return;
        }
        try {
            this.run();
            this.inStream.close();
            this.inStream = null;
            this.outStream.close();
            this.outStream = null;
            this.socket.close();
            this.socket = null;
        } catch (IOException e) {
            System.out.println("Engine disconnected.");
        }
    }
}