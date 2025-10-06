package src.core;

public class Game {
    private final Board board = new Board();
    private boolean whiteTurn = true;

    public void start(){
        board.display();
    }

    public boolean makeMove(String fromStr, String toStr){
        Position from = Position.fromAlgebraic(fromStr);
        Position to = Position.fromAlgebraic(toStr);
        return board.movePiece(from, to);
    }

    public void nextTurn() { whiteTurn = !whiteTurn; }
    public boolean isWhite() { return whiteTurn; };
}
