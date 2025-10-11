package pieces;

import board.Board;
import utils.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * King can move one square in any direction.
 * Castling is handled by Board when move attempts are made.
 */
public class King extends Piece {
    public King(utils.Color color, Position position) {
        super(color, position);
    }

    @Override
    protected String getLetter() {
        return "K";
    }

    @Override
    public List<Position> possibleMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        int[] dRow = {-1,-1,-1,0,0,1,1,1};
        int[] dCol = {-1,0,1,-1,1,-1,0,1};
        for (int k = 0; k < dRow.length; k++) {
            Position p = new Position(position.row + dRow[k], position.col + dCol[k]);
            if (!p.inBounds()) continue;
            if (board.getPiece(p) == null || board.getPiece(p).getColor() != color) {
                moves.add(p);
            }
        }
        // Castling target squares added by Board.getCastlingTargets (board-level logic)
        moves.addAll(board.getCastlingTargets(this));
        return moves;
    }
}
