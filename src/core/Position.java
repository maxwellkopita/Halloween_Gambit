package src.core;

public record Position(int file, int rank) {
    public static Position fromAlgebraic(String s) {
        s = s.toUpperCase();
        return new Position(s.charAt(0) - 'A', s.charAt(1) - '1');
    }

    public String toAlgebraic() {
        return "" + (char)('A' + file) + (rank + 1);
    }
}
