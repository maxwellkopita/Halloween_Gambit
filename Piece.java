package pieces;

import utils.Color;
import utils.Position;
import board.Board;

import java.util.List;

/**
 * Abstract base class for chess pieces.
 */
public abstract class Piece {
    protected Color color;
    protected Position position;
    protected String shortName; // like "wP", "bN"
    protected boolean hasMoved = false;

    public Piece(Color color, Position position) {
        this.color = color;
        this.position = position;
        this.shortName = (color == Color.WHITE ? "w" : "b") + getLetter();
    }

    /**
     * Return letter code for piece type (P,N,B,R,Q,K).
     */
    protected abstract String getLetter();

    /**
     * Returns a list of possible moves for this piece from current position.
     * Does not check for leaving king in check. Use Board.getLegalMoves to filter.
     */
    public abstract List<Position> possibleMoves(Board board);

    public Color getColor() {
        return color;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position p) {
        this.position = p;
    }

    public String getShortName() {
        return shortName;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean moved) {
        this.hasMoved = moved;
    }

    @Override
    public String toString() {
        return shortName;
    }
}
