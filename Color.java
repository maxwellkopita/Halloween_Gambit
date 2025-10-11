package utils;

/**
 * Enum for piece colors.
 */
public enum Color {
    WHITE, BLACK;

    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    @Override
    public String toString() {
        return this == WHITE ? "White" : "Black";
    }
}
