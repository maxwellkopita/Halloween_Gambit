package pieces;

import board.Board;
import utils.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Knight moves in L-shape.
 */
public class Knight extends Piece {
    public Knight(utils.Color color, Position position) {
        super(color, position);
    }

    @Override
    protected String getLetter() {
        return "N";
    }

    @Override
    public List<Position> possibleMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        int[] dRow = {-2,-2,-1,-1,1,1,2,2};
        int[] dCol = {-1,1,-2,2,-2,2,-1,1};
        for (int k = 0; k < dRow.length; k++) {
            int r = position.row + dRow[k];
            int c = position.col + dCol[k];
            Position p = new Position(r, c);
            if (!p.inBounds()) continue;
            if (board.getPiece(p) == null || board.getPiece(p).getColor() != color) {
                moves.add(p);
            }
        }
        return moves;
    }
}
