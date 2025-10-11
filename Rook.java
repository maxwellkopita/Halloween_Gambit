package pieces;

import board.Board;
import utils.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Rook moves along ranks/files until blocked.
 */
public class Rook extends Piece {
    public Rook(utils.Color color, Position position) {
        super(color, position);
    }

    @Override
    protected String getLetter() {
        return "R";
    }

    @Override
    public List<Position> possibleMoves(Board board) {
        List<Position> moves = new ArrayList<>();
        int[] dRow = {-1, 1, 0, 0};
        int[] dCol = {0, 0, -1, 1};
        for (int k = 0; k < 4; k++) {
            int r = position.row + dRow[k];
            int c = position.col + dCol[k];
            while (r >= 0 && r < 8 && c >= 0 && c < 8) {
                Position p = new Position(r, c);
                if (board.getPiece(p) == null) {
                    moves.add(p);
                } else {
                    if (board.getPiece(p).getColor() != color) {
                        moves.add(p);
                    }
                    break;
                }
                r += dRow[k];
                c += dCol[k];
            }
        }
        return moves;
    }
}
