package board;

import utils.Position;

/**
 * Simple move record: from-to and optional promotion piece letter.
 */
public class Move {
    public final Position from;
    public final Position to;
    public final Character promotion; // 'Q','R','B','N' or null

    public Move(Position from, Position to) {
        this(from, to, null);
    }

    public Move(Position from, Position to, Character promotion) {
        this.from = from;
        this.to = to;
        this.promotion = promotion;
    }

    @Override
    public String toString() {
        if (promotion != null) return from.toAlgebraic() + "-" + to.toAlgebraic() + "=" + promotion;
        return from.toAlgebraic() + "-" + to.toAlgebraic();
    }
}
