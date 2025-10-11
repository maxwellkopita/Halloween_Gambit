package pieces;

import board.Board;
import utils.Color;
import utils.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Pawn piece. Implements forward moves and captures, used for both movement and attack squares.
 * En passant and promotion are handled in Board/Game logic (Board keeps enPassantTarget).
 */
public class Pawn extends Piece {

    public Pawn(Color color, Position position) {
        super(color, position);
    }

    @Override
    protected String getLetter() {
        return "P";
    }

    @Override
    public List<Position> possibleMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        int dir = (color == Color.WHITE) ? -1 : 1; // white moves "up" towards row 0
        int startRow = (color == Color.WHITE) ? 6 : 1;

        // one forward
        Position oneF = new Position(position.row + dir, position.col);
        if (oneF.inBounds() && board.getPiece(oneF) == null) {
            moves.add(oneF);

            // two forward from start row
            Position twoF = new Position(position.row + 2 * dir, position.col);
            if (position.row == startRow && twoF.inBounds() && board.getPiece(twoF) == null) {
                moves.add(twoF);
            }
        }

        // captures (diagonals)
        Position capL = new Position(position.row + dir, position.col - 1);
        Position capR = new Position(position.row + dir, position.col + 1);
        if (capL.inBounds()) {
            if (board.getPiece(capL) != null && board.getPiece(capL).getColor() != color) {
                moves.add(capL);
            }
            // en-passant: capture to capL if enPassantTarget equals capL
            if (board.getEnPassantTarget() != null && board.getEnPassantTarget().equals(capL)) {
                moves.add(capL);
            }
        }
        if (capR.inBounds()) {
            if (board.getPiece(capR) != null && board.getPiece(capR).getColor() != color) {
                moves.add(capR);
            }
            if (board.getEnPassantTarget() != null && board.getEnPassantTarget().equals(capR)) {
                moves.add(capR);
            }
        }

        return moves;
    }
}
