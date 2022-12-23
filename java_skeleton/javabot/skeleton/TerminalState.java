package javabot.skeleton;

import java.util.List;
import java.util.Collections;
import java.lang.Integer;

/**
 * Final state of a poker round corresponding to payoffs.
 */
public class TerminalState extends State {
    public final List<Integer> deltas;
    public final State previousState;

    public TerminalState(List<Integer> deltas, State previousState) {
        this.deltas = Collections.unmodifiableList(deltas);
        this.previousState = previousState;
    }
}