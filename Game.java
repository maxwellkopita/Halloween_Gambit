package game;

import board.Board;
import board.Move;
import java.util.List;
import java.util.Scanner;
import pieces.*;
import utils.Color;
import utils.Position;

/**
 * Main game loop for console chess with castling, en-passant, promotion, and check/checkmate detection.
 */
public class Game {

    private Board board;
    private Color currentTurn;

    public Game() {
        board = new Board();
        currentTurn = Color.WHITE;
    }

    public void start() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Welcome to Console Chess (Phase 2). Moves like: E2 E4, O-O, O-O-O, E7 E8=Q");
        while (true) {
            board.display();
            if (board.isCheck(currentTurn)) {
                System.out.println(currentTurn + " is in CHECK!");
            }
            System.out.println(currentTurn + "'s turn. Enter move (or 'quit'):");
            System.out.print("> ");
            String line = sc.nextLine().trim();
            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("q")) {
                System.out.println("Game ended by user.");
                break;
            }
            if (line.isEmpty()) continue;

            try {
                // Castling shortcuts
                if (line.equalsIgnoreCase("O-O") || line.equalsIgnoreCase("0-0") ) {
                    handleCastling(true);
                    if (board.isCheckmate(currentTurn.opposite())) {
                        board.display();
                        System.out.println(currentTurn + " wins by checkmate!");
                        break;
                    }
                    currentTurn = currentTurn.opposite();
                    continue;
                } else if (line.equalsIgnoreCase("O-O-O") || line.equalsIgnoreCase("0-0-0")) {
                    handleCastling(false);
                    if (board.isCheckmate(currentTurn.opposite())) {
                        board.display();
                        System.out.println(currentTurn + " wins by checkmate!");
                        break;
                    }
                    currentTurn = currentTurn.opposite();
                    continue;
                }

                // parse promotion suffix if any
                Character promotion = null;
                String movePart = line;
                if (line.contains("=")) {
                    String[] parts = line.split("=");
                    movePart = parts[0].trim();
                    char pch = parts[1].trim().toUpperCase().charAt(0);
                    promotion = pch;
                }

                String[] tokens = movePart.split("\\s+");
                if (tokens.length != 2) {
                    System.out.println("Invalid input. Use format: E2 E4 or O-O or E7 E8=Q");
                    continue;
                }
                Position from = Position.fromAlgebraic(tokens[0]);
                Position to = Position.fromAlgebraic(tokens[1]);
                Piece p = board.getPiece(from);
                if (p == null) {
                    System.out.println("No piece at " + tokens[0]);
                    continue;
                }
                if (p.getColor() != currentTurn) {
                    System.out.println("That's not your piece. It's " + currentTurn + "'s turn.");
                    continue;
                }

                // get legal moves for this piece (considers leaving king in check, castling legality)
                List<Position> legal = board.getLegalMovesForPiece(p);
                boolean allowed = false;
                for (Position pos : legal) {
                    if (pos.equals(to)) { allowed = true; break; }
                }
                if (!allowed) {
                    System.out.println("Illegal move for that piece (or move would leave king in check).");
                    continue;
                }

                // build Move (include promotion char if provided)
                Move mv = new Move(from, to, promotion);

                // Execute
                board.executeMove(mv);

                // handle pawn promotion if no promotion char provided earlier and pawn reached last rank
                Piece moved = board.getPiece(to);
                if (moved instanceof Pawn) {
                    boolean whitePromo = moved.getColor() == Color.WHITE && to.row == 0;
                    boolean blackPromo = moved.getColor() == Color.BLACK && to.row == 7;
                    if ((whitePromo || blackPromo) && mv.promotion == null) {
                        // prompt for promotion
                        System.out.println("Pawn reached promotion rank. Choose promotion (Q/R/B/N):");
                        System.out.print("> ");
                        String choice = sc.nextLine().trim().toUpperCase();
                        char c = (choice.isEmpty() ? 'Q' : choice.charAt(0));
                        Move promoMove = new Move(from, to, c);
                        // Re-execute promotion by re-setting the piece at 'to'
                        // We can overwrite the pawn with promoted piece:
                        board.executeMove(new Move(to, to, c)); // trick: executeMove will replace pawn at 'to' if promotion present
                    }
                }

                // after move check for checkmate/stalemate
                if (board.isCheckmate(currentTurn.opposite())) {
                    board.display();
                    System.out.println(currentTurn + " wins by checkmate!");
                    break;
                }

                // switch turn
                currentTurn = currentTurn.opposite();

            } catch (IllegalArgumentException ex) {
                System.out.println("Invalid move input: " + ex.getMessage());
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        sc.close();
    }

    private void handleCastling(boolean kingside) {
        // find king of currentTurn
        Piece king = null;
        Position kingPos = null;
        for (Piece p : board.getAllPieces()) {
            if (p instanceof King && p.getColor() == currentTurn) {
                king = p;
                kingPos = p.getPosition();
                break;
            }
        }
        if (king == null) {
            System.out.println("King not found!");
            return;
        }
        List<Position> targets = board.getCastlingTargets(king);
        Position desired = new Position(kingPos.row, kingPos.col + (kingside ? 2 : -2));
        boolean allowed = false;
        for (Position t : targets) if (t.equals(desired)) allowed = true;
        if (!allowed) {
            System.out.println("Castling not allowed (blocked, moved pieces, or rook missing).");
            return;
        }
        // additionally ensure the squares king passes through are not attacked (cannot castle through check)
        int dir = kingside ? 1 : -1;
        Position pass1 = new Position(kingPos.row, kingPos.col + dir);
        Position pass2 = new Position(kingPos.row, kingPos.col + 2*dir);
        // check current, pass1, destination are not under attack
        if (board.isSquareAttacked(kingPos, currentTurn.opposite()) ||
            board.isSquareAttacked(pass1, currentTurn.opposite()) ||
            board.isSquareAttacked(pass2, currentTurn.opposite())) {
            System.out.println("Castling not allowed through or into check.");
            return;
        }
        // execute castling move (king move will move rook inside executeMove)
        board.executeMove(new Move(kingPos, pass2, null));
    }

    public static void main(String[] args) {
        new Game().start();
    }
}
