package board;

import pieces.*;
import utils.Color;
import utils.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Board class holds the 8x8 board and implements operations including:
 * - standard setup
 * - move execution with castling, en-passant, promotion support
 * - check / checkmate detection
 * - filtering of legal moves (removes moves that leave own king in check)
 */
public class Board {
    private Piece[][] grid;
    private List<Piece> captured;
    private Position enPassantTarget; // square that can be captured via en-passant (if any)
    private Move lastMove; // last executed move

    public Board() {
        grid = new Piece[8][8];
        captured = new ArrayList<>();
        initializeStandardSetup();
        enPassantTarget = null;
        lastMove = null;
    }

    /**
     * Deep copy constructor.
     */
    public Board(Board other) {
        grid = new Piece[8][8];
        captured = new ArrayList<>(other.captured);
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = other.grid[r][c];
                if (p != null) {
                    // recreate appropriate piece type with same state
                    Position pos = new Position(r, c);
                    Piece copy;
                    if (p instanceof Pawn) copy = new Pawn(p.getColor(), pos);
                    else if (p instanceof Rook) copy = new Rook(p.getColor(), pos);
                    else if (p instanceof Knight) copy = new Knight(p.getColor(), pos);
                    else if (p instanceof Bishop) copy = new Bishop(p.getColor(), pos);
                    else if (p instanceof Queen) copy = new Queen(p.getColor(), pos);
                    else if (p instanceof King) copy = new King(p.getColor(), pos);
                    else throw new IllegalStateException("Unknown piece type");
                    copy.setHasMoved(p.hasMoved());
                    grid[r][c] = copy;
                }
            }
        }
        this.enPassantTarget = other.enPassantTarget == null ? null : new Position(other.enPassantTarget.row, other.enPassantTarget.col);
        this.lastMove = other.lastMove; // Move is immutable enough for simulation
    }

    /**
     * Initialize pieces into standard starting positions.
     */
    public void initializeStandardSetup() {
        // Clear
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) grid[r][c] = null;

        // Pawns
        for (int c = 0; c < 8; c++) {
            grid[1][c] = new Pawn(Color.BLACK, new Position(1, c));
            grid[6][c] = new Pawn(Color.WHITE, new Position(6, c));
        }
        // Rooks
        grid[0][0] = new Rook(Color.BLACK, new Position(0,0));
        grid[0][7] = new Rook(Color.BLACK, new Position(0,7));
        grid[7][0] = new Rook(Color.WHITE, new Position(7,0));
        grid[7][7] = new Rook(Color.WHITE, new Position(7,7));

        // Knights
        grid[0][1] = new Knight(Color.BLACK, new Position(0,1));
        grid[0][6] = new Knight(Color.BLACK, new Position(0,6));
        grid[7][1] = new Knight(Color.WHITE, new Position(7,1));
        grid[7][6] = new Knight(Color.WHITE, new Position(7,6));

        // Bishops
        grid[0][2] = new Bishop(Color.BLACK, new Position(0,2));
        grid[0][5] = new Bishop(Color.BLACK, new Position(0,5));
        grid[7][2] = new Bishop(Color.WHITE, new Position(7,2));
        grid[7][5] = new Bishop(Color.WHITE, new Position(7,5));

        // Queens
        grid[0][3] = new Queen(Color.BLACK, new Position(0,3));
        grid[7][3] = new Queen(Color.WHITE, new Position(7,3));

        // Kings
        grid[0][4] = new King(Color.BLACK, new Position(0,4));
        grid[7][4] = new King(Color.WHITE, new Position(7,4));

        // ensure hasMoved defaults false
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            if (grid[r][c] != null) grid[r][c].setHasMoved(false);
        }
    }

    public Piece getPiece(Position p) {
        if (p == null || !p.inBounds()) return null;
        return grid[p.row][p.col];
    }

    /**
     * Execute a move (assumes validated and legal). Handles:
     * - captures
     * - en-passant
     * - castling
     * - promotion (if 'promotion' char provided)
     */
    public void executeMove(Move move) {
        Position from = move.from;
        Position to = move.to;
        Piece moving = getPiece(from);
        if (moving == null) throw new IllegalArgumentException("No piece at " + from.toAlgebraic());

        // Castling: king moves two squares horizontally
        if (moving instanceof King && Math.abs(to.col - from.col) == 2) {
            boolean kingside = to.col > from.col;
            // move rook accordingly
            int rookFromCol = kingside ? 7 : 0;
            int rookToCol = kingside ? to.col - 1 : to.col + 1;
            Piece rook = grid[from.row][rookFromCol];
            if (rook == null || !(rook instanceof Rook)) {
                throw new IllegalStateException("Castling rook missing");
            }
            // move king and rook
            grid[to.row][to.col] = moving;
            grid[from.row][from.col] = null;
            moving.setPosition(to);
            moving.setHasMoved(true);

            grid[from.row][rookFromCol] = null;
            grid[to.row][rookToCol] = rook;
            rook.setPosition(new Position(to.row, rookToCol));
            rook.setHasMoved(true);
            // clear en-passant target
            enPassantTarget = null;
            lastMove = move;
            return;
        }

        // En-passant: pawn moves diagonal onto enPassantTarget but target square is empty -> capture pawn behind
        if (moving instanceof Pawn) {
            if (enPassantTarget != null && to.equals(enPassantTarget) && getPiece(to) == null) {
                // captured pawn is behind the target square
                int dir = (moving.getColor() == Color.WHITE) ? 1 : -1; // captured pawn sits one row "behind" target
                Position capturedPawnPos = new Position(to.row + dir, to.col);
                Piece capturedPawn = getPiece(capturedPawnPos);
                if (capturedPawn != null && capturedPawn instanceof Pawn && capturedPawn.getColor() != moving.getColor()) {
                    captured.add(capturedPawn);
                    grid[capturedPawnPos.row][capturedPawnPos.col] = null;
                }
            }
        }

        // Regular capture
        Piece target = getPiece(to);
        if (target != null) {
            captured.add(target);
        }

        // Move piece
        grid[to.row][to.col] = moving;
        grid[from.row][from.col] = null;
        moving.setPosition(to);

        // Pawn double-step sets enPassantTarget
        enPassantTarget = null;
        if (moving instanceof Pawn) {
            if (Math.abs(to.row - from.row) == 2) {
                // en passant target square is the square "between" from & to
                int midRow = (to.row + from.row) / 2;
                enPassantTarget = new Position(midRow, to.col);
            }
        }

        // Promotion if requested
        if (moving instanceof Pawn) {
            boolean whitePromo = moving.getColor() == Color.WHITE && to.row == 0;
            boolean blackPromo = moving.getColor() == Color.BLACK && to.row == 7;
            if ((whitePromo || blackPromo) && move.promotion != null) {
                char pchar = Character.toUpperCase(move.promotion);
                Piece promoted;
                switch (pchar) {
                    case 'Q': promoted = new Queen(moving.getColor(), to); break;
                    case 'R': promoted = new Rook(moving.getColor(), to); break;
                    case 'B': promoted = new Bishop(moving.getColor(), to); break;
                    case 'N': promoted = new Knight(moving.getColor(), to); break;
                    default: promoted = new Queen(moving.getColor(), to); break;
                }
                grid[to.row][to.col] = promoted;
            }
        }

        moving.setHasMoved(true);
        lastMove = move;
    }

    /**
     * Returns list of castling target squares for king (empty list if none).
     * This function is consulted when building King's possibleMoves; final legality checked by filtering legal moves.
     */
    public List<Position> getCastlingTargets(Piece king) {
        List<Position> targets = new ArrayList<>();
        if (!(king instanceof King)) return targets;
        if (king.hasMoved()) return targets;

        int row = king.getPosition().row;
        // kingside
        Piece rookK = grid[row][7];
        if (rookK != null && rookK instanceof Rook && !rookK.hasMoved()) {
            // squares between king and rook must be empty
            boolean empty = true;
            for (int c = king.getPosition().col + 1; c < 7; c++) {
                if (grid[row][c] != null) { empty = false; break; }
            }
            if (empty) targets.add(new Position(row, king.getPosition().col + 2)); // king moves two right
        }
        // queenside
        Piece rookQ = grid[row][0];
        if (rookQ != null && rookQ instanceof Rook && !rookQ.hasMoved()) {
            boolean empty = true;
            for (int c = 1; c < king.getPosition().col; c++) {
                if (grid[row][c] != null) { empty = false; break; }
            }
            if (empty) targets.add(new Position(row, king.getPosition().col - 2)); // king moves two left
        }
        return targets;
    }

    /**
     * Returns whether a square is attacked by the given color.
     */
    public boolean isSquareAttacked(Position sq, Color byColor) {
        // iterate all pieces of byColor and see if any have sq among their "attack" squares
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                if (p == null || p.getColor() != byColor) continue;
                // For pawns, their possibleMoves include captures and en-passant; but when checking attacks we must consider capture diagonals even if destination empty.
                if (p instanceof Pawn) {
                    int dir = (p.getColor() == Color.WHITE) ? -1 : 1;
                    Position left = new Position(r + dir, c - 1);
                    Position right = new Position(r + dir, c + 1);
                    if (left.inBounds() && left.equals(sq)) return true;
                    if (right.inBounds() && right.equals(sq)) return true;
                } else {
                    List<Position> moves = p.possibleMoves(this);
                    for (Position pos : moves) {
                        // Note: possibleMoves for sliding pieces stops at first capture; that's correct for attack detection.
                        if (pos.equals(sq)) return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Return true if color's king is in check.
     */
    public boolean isCheck(Color color) {
        Position kingPos = findKing(color);
        if (kingPos == null) return false; // should not happen
        return isSquareAttacked(kingPos, color.opposite());
    }

    /**
     * Return true if color is in checkmate.
     * (In check and no legal move prevents check)
     */
    public boolean isCheckmate(Color color) {
        if (!isCheck(color)) return false;
        // if any legal move for color results in king not in check => not checkmate
        for (Piece p : getAllPieces()) {
            if (p.getColor() != color) continue;
            List<Position> cand = p.possibleMoves(this);
            for (Position to : cand) {
                Move mv = new Move(p.getPosition(), to, null);
                Board copy = new Board(this);
                try {
                    copy.executeMove(mv);
                } catch (Exception e) {
                    continue;
                }
                if (!copy.isCheck(color)) return false;
            }
        }
        return true;
    }

    /**
     * Find the king position for color.
     */
    private Position findKing(Color color) {
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            Piece p = grid[r][c];
            if (p != null && p instanceof King && p.getColor() == color) return new Position(r, c);
        }
        return null;
    }

    /**
     * Return list of all pieces on board.
     */
    public List<Piece> getAllPieces() {
        List<Piece> res = new ArrayList<>();
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) if (grid[r][c] != null) res.add(grid[r][c]);
        return res;
    }

    /**
     * Return all legal moves for a piece (filters out moves that would leave own king in check).
     */
    public List<Position> getLegalMovesForPiece(Piece piece) {
        List<Position> res = new ArrayList<>();
        for (Position to : piece.possibleMoves(this)) {
            Move mv = new Move(piece.getPosition(), to, null);
            Board copy = new Board(this);
            try {
                copy.executeMove(mv);
            } catch (Exception e) {
                continue;
            }
            if (!copy.isCheck(piece.getColor())) res.add(to);
        }
        return res;
    }

    public Position getEnPassantTarget() { return enPassantTarget; }

    public Move getLastMove() { return lastMove; }

    /**
     * Prints board to console.
     */
    public void display() {
        System.out.println();
        // Top labels
        System.out.print("   ");
        for (int c = 0; c < 8; c++) {
            System.out.print(" " + (char)('A' + c) + " ");
        }
        System.out.println();
        for (int r = 0; r < 8; r++) {
            int rank = 8 - r;
            System.out.print(" " + rank + " ");
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                if (p == null) {
                    System.out.print("## ");
                } else {
                    System.out.print(p.getShortName() + " ");
                }
            }
            System.out.print(" " + rank);
            System.out.println();
        }
        // Bottom labels
        System.out.print("   ");
        for (int c = 0; c < 8; c++) {
            System.out.print(" " + (char)('A' + c) + " ");
        }
        System.out.println("\n");
    }
}
