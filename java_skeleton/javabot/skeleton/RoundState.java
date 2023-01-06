package javabot.skeleton;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.lang.Integer;
import java.lang.String;

/**
 * Encodes the game tree for one round of poker.
 */
public class RoundState extends State {
    public final int button;
    public final int street;
    public final List<Integer> pips;
    public final List<Integer> stacks;
    public final List<List<String>> hands;
    public final List<String> deck;
    public final State previousState;

    public RoundState(int button, int street, List<Integer> pips, List<Integer> stacks,
                      List<List<String>> hands, List<String> deck, State previousState) {
        this.button = button;
        this.street = street;
        this.pips = Collections.unmodifiableList(pips);
        this.stacks = Collections.unmodifiableList(stacks);
        this.hands = Collections.unmodifiableList(
            Arrays.asList(
                Collections.unmodifiableList(hands.get(0)),
                Collections.unmodifiableList(hands.get(1))
            )
        );
        this.deck = Collections.unmodifiableList(deck);
        this.previousState = previousState;
    }

    /**
     * Compares the players' hands and computes payoffs.
     */
    public State showdown() {
        return new TerminalState(Arrays.asList(0, 0), this);
    }

    /**
     * Returns the active player's legal moves.
     */
    public Set<ActionType> legalActions() {
        int active = this.button % 2;
        int continueCost = this.pips.get(1-active) - this.pips.get(active);
        if (continueCost == 0) {
            // we can only raise the stakes if both players can afford it
            boolean betsForbidden = ((this.stacks.get(0) == 0) | (this.stacks.get(1) == 0));
            if (betsForbidden) {
                return new HashSet<ActionType>(Arrays.asList(ActionType.CHECK_ACTION_TYPE));
            }
            return new HashSet<ActionType>(Arrays.asList(ActionType.CHECK_ACTION_TYPE, ActionType.RAISE_ACTION_TYPE));
        }
        // continueCost > 0
        // similarly, re-raising is only allowed if both players can afford it
        boolean raisesForbidden = ((continueCost == this.stacks.get(active)) | (this.stacks.get(1-active) == 0));
        if (raisesForbidden) {
            return new HashSet<ActionType>(Arrays.asList(ActionType.FOLD_ACTION_TYPE, ActionType.CALL_ACTION_TYPE));
        }
        return new HashSet<ActionType>(Arrays.asList(ActionType.FOLD_ACTION_TYPE,
                                                     ActionType.CALL_ACTION_TYPE,
                                                     ActionType.RAISE_ACTION_TYPE));
    }

    /**
     * Returns a list of the minimum and maximum legal raises.
     */
    public List<Integer> raiseBounds() {
        int active = this.button % 2;
        int continueCost = this.pips.get(1-active) - this.pips.get(active);
        int maxContribution = Math.min(this.stacks.get(active), this.stacks.get(1-active) + continueCost);
        int minContribution = Math.min(maxContribution, continueCost + Math.max(continueCost, State.BIG_BLIND));
        return Arrays.asList(this.pips.get(active) + minContribution, this.pips.get(active) + maxContribution);
    }

    /**
     * Resets the players' pips and advances the game tree to the next round of betting.
     */
    public State proceedStreet() {
        // if (this.street == 5) {
        //     return this.showdown();
        // }
        int newStreet;
        if (this.street == 0) {
            newStreet = 3;
        } else {
            newStreet = this.street + 1;
        }
        return new RoundState(1, newStreet, Arrays.asList(0, 0), this.stacks, this.hands, this.deck, this);
    }

    /**
     * Advances the game tree by one action performed by the active player.
     */
    public State proceed(Action action) {
        int active = this.button % 2;
        switch (action.actionType) {
            case FOLD_ACTION_TYPE: {
                int delta;
                if (active == 0) {
                    delta = this.stacks.get(0) - State.STARTING_STACK;
                } else {
                    delta = State.STARTING_STACK - this.stacks.get(1);
                }
                return new TerminalState(Arrays.asList(delta, -1 * delta), this);
            }
            case CALL_ACTION_TYPE: {
                if (this.button == 0) {  // sb calls bb
                    return new RoundState(1, 0, Arrays.asList(State.BIG_BLIND, State.BIG_BLIND),
                                          Arrays.asList(State.STARTING_STACK - State.BIG_BLIND,
                                                        State.STARTING_STACK - State.BIG_BLIND),
                                          this.hands, this.deck, this);
                }
                // both players acted
                List<Integer> newPips = new ArrayList<Integer>(this.pips);
                List<Integer> newStacks = new ArrayList<Integer>(this.stacks);
                int contribution = newPips.get(1-active) - newPips.get(active);
                newStacks.set(active, newStacks.get(active) - contribution);
                newPips.set(active, newPips.get(active) + contribution);
                RoundState state = new RoundState(this.button + 1, this.street, newPips, newStacks,
                                                  this.hands, this.deck, this);
                return state.proceedStreet();
            }
            case CHECK_ACTION_TYPE: {
                if (((this.street == 0) & (this.button > 0)) | (this.button > 1)) {  // both players acted
                    return this.proceedStreet();
                }
                // let opponent act
                return new RoundState(this.button + 1, this.street, this.pips, this.stacks, this.hands, this.deck, this);
            }
            default: {  // RAISE_ACTION_TYPE
                List<Integer> newPips = new ArrayList<Integer>(this.pips);
                List<Integer> newStacks = new ArrayList<Integer>(this.stacks);
                int contribution = action.amount - newPips.get(active);
                newStacks.set(active, newStacks.get(active) - contribution);
                newPips.set(active, newPips.get(active) + contribution);
                return new RoundState(this.button + 1, this.street, newPips, newStacks, this.hands, this.deck, this);
            }
        }
    }
}