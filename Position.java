package utils;

/**
 * Represents a board position (row, column). Row and column are 0-based internally.
 * Externally coordinates use files A-H and ranks 1-8.
 */
public class Position {
    public final int row;    // 0..7 (0 is top row on display -> rank 8)
    public final int col;    // 0..7 (0 is file A)

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * Convert algebraic (e.g. "E2") to Position. Throws IllegalArgumentException on bad input.
     */
    public static Position fromAlgebraic(String sq) {
        if (sq == null || sq.length() < 2) {
            throw new IllegalArgumentException("Invalid square: " + sq);
        }
        sq = sq.trim().toUpperCase();
        char file = sq.charAt(0);
        char rank = sq.charAt(1);
        if (file < 'A' || file > 'H' || rank < '1' || rank > '8') {
            throw new IllegalArgumentException("Invalid square: " + sq);
        }
        int col = file - 'A';
        int row = 8 - (rank - '0'); // rank '1' => row 7, rank '8' => row 0
        return new Position(row, col);
    }

    public String toAlgebraic() {
        char file = (char) ('A' + col);
        char rank = (char) ('0' + (8 - row));
        return "" + file + rank;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Position)) return false;
        Position p = (Position) o;
        return this.row == p.row && this.col == p.col;
    }

    @Override
    public int hashCode() {
        return row * 31 + col;
    }

    public boolean inBounds() {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }
}
