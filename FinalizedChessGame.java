import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.io.*;
import java.util.*;

/*
 * Single-file Java Swing Chess GUI with:
 * - Drag-and-drop piece movement
 * - Undo last move
 * - Move history in coordinate notation (A1, ...)
 * - White/Black captured piece graveyards with dynamic font scaling
 * - Board light/dark color settings
 * - Piece color customization (live)
 * - Board size selection (Small, Medium, Large)
 * - Save / Load game
 * - Detect King capture (endgame)
 
 * This implementation uses Unicode chess glyphs for piece drawing and implements
 * standard chess rules: legal move generation, castling, en-passant, pawn promotion
 * (promotes to queen by default with UI prompt), and move legality with check prevention.
 */

// This class is the generalization of the Chess Game skeleton
public class ChessGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChessFrame().setVisible(true));
    }
}

// This class is the framework involved with how the chess game looks and feels
public class ChessFrame extends JFrame {
    BoardPanel boardPanel;
    GameState state;
    DefaultListModel<String> historyModel = new DefaultListModel<>();

    // This JList shows the history of the moves made on the GUI
    JList<String> historyList;

    // The graveyard feature showing which pieces were captured
    JPanel whiteGrave, blackGrave;

    // This dynamically changes the size of the chess board between (small, medium and large)
    JComboBox<String> sizeCombo;

    // These are the buttons that you can click on the chess game GUI
    JButton undoBtn, saveBtn, loadBtn, newGameBtn;

    // Setting the color of the board and the pieces when the user launches the chess game GUI
    Color lightColor = new Color(240, 217, 181);
    Color darkColor = new Color(181, 136, 99);
    Color whitePieceColor = Color.WHITE;
    Color blackPieceColor = Color.BLACK;

    public ChessFrame() {
        // These setter methods set the title of the GUI on the top of the screen
        setTitle("Java Chess Game GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 780);
        setLocationRelativeTo(null);

        state = new GameState();
        boardPanel = new BoardPanel(state, this);

        // RIGHT PANEL which displays the different buttons which you can interact with to do certain actions
        JPanel right = new JPanel();
        right.setLayout(new BorderLayout());
        right.setPreferredSize(new Dimension(400, 0));

        // This shows the different moves made in chess coordinates eg. A2 -> A4
        historyList = new JList<>(historyModel);
        JScrollPane histScroll = new JScrollPane(historyList);
        histScroll.setBorder(BorderFactory.createTitledBorder("Move History"));
        right.add(histScroll, BorderLayout.CENTER);

        // This structures the chess game to look more refined
        JPanel topControls = new JPanel();
        topControls.setLayout(new BoxLayout(topControls, BoxLayout.Y_AXIS));
        topControls.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        // This is the undo button which erases the most recent move that has been stored
        undoBtn = new JButton("Undo Last Move");
        undoBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        undoBtn.addActionListener(e -> {
            if (state.undoLast()) {
                if (!historyModel.isEmpty()) historyModel.remove(historyModel.size()-1);
                boardPanel.repaint();

                // We update the graveyard in case a piece was captured and undoing the capture removes the piece from the graveyard
                updateGraveyards();
            }
        });
        // Adds the undoBtn in the GUI
        topControls.add(undoBtn);
        topControls.add(Box.createVerticalStrut(5));

        // This enables the use to change the pixels in which the board is shown on the GUI
        // Left alignment because the right panel has user buttons that can be pressed
        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sizePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // This provides a drop down menu which has the options small, medium, large
        sizePanel.setBorder(BorderFactory.createTitledBorder("Board Size"));
        sizePanel.add(new JLabel("Select:"));
        sizeCombo = new JComboBox<>(new String[]{"Small","Medium","Large"});

        // This dynamically updates the size of the board to the newSize selected by the user
        sizeCombo.setSelectedIndex(1);
        sizeCombo.addActionListener(e -> {
            String s = (String)sizeCombo.getSelectedItem();
            int newSize = s.equals("Small") ? 50 : s.equals("Medium") ? 70 : 90;
            boardPanel.setSquareSize(newSize);
            boardPanel.setPreferredSize(new Dimension(8*newSize, 8*newSize));
            boardPanel.revalidate();
            pack();
        });
        sizePanel.add(sizeCombo);
        topControls.add(sizePanel);
        topControls.add(Box.createVerticalStrut(5));

        // This is another box on the right side of the GUI which consists of the piece colors
        // This is where the user can select either white pieces or black pieces
        JPanel pieceColorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pieceColorPanel.setBorder(BorderFactory.createTitledBorder("Piece Colors"));
        pieceColorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Here are the buttons where you are able to customize the color of the pieces themselves
        // This will show up on the chessboard itself and allow for some funky looking gameplay
        JButton whiteBtn = new JButton("White Pieces");
        whiteBtn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "White Pieces Color", whitePieceColor);
            if (c != null) { whitePieceColor = c; boardPanel.repaint(); }
        });
        JButton blackBtn = new JButton("Black Pieces");
        blackBtn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Black Pieces Color", blackPieceColor);
            if (c != null) { blackPieceColor = c; boardPanel.repaint(); }
        });
        pieceColorPanel.add(whiteBtn); pieceColorPanel.add(blackBtn);
        topControls.add(pieceColorPanel);
        topControls.add(Box.createVerticalStrut(5));

        // Here are the buttons where you are able to customize the color of the squares themselves
        // This will show up on the chessboard itself and allow for some funky looking gameplay
        JPanel boardColorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        boardColorPanel.setBorder(BorderFactory.createTitledBorder("Board Colors"));
        boardColorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton lightSquareBtn = new JButton("Light Squares");
        lightSquareBtn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Light Square Color", lightColor);
            if (c != null) { lightColor = c; boardPanel.repaint(); }
        });
        JButton darkSquareBtn = new JButton("Dark Squares");
        darkSquareBtn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Dark Square Color", darkColor);
            if (c != null) { darkColor = c; boardPanel.repaint(); }
        });
        boardColorPanel.add(lightSquareBtn); boardColorPanel.add(darkSquareBtn);
        topControls.add(boardColorPanel);
        topControls.add(Box.createVerticalStrut(5));

        // This is another box but this one contains options that can completely change the game
        // Features a new game button which erases the history of the current game
        // Features a save game button which can be stored on your computer
        // Features a load game button which can load previous saved games with their colors included
        JPanel gameSettingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gameSettingsPanel.setBorder(BorderFactory.createTitledBorder("Game Controls"));
        gameSettingsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        saveBtn = new JButton("Save Game");
        saveBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) {
                try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fc.getSelectedFile()))) {
                    out.writeObject(state);
                    out.writeObject(Collections.list(historyModel.elements()));
                } catch(Exception ex){ ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Failed to save: "+ex); }
            }
        });

        loadBtn = new JButton("Load Game");
        loadBtn.addActionListener(e -> {
    JFileChooser fc = new JFileChooser();
    if (fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) {
        try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(fc.getSelectedFile()))) {

            GameState loaded = (GameState)in.readObject();
            @SuppressWarnings("unchecked")
            java.util.List<String> hist = (java.util.List<String>)in.readObject();

            // This restores the colors of the squares and pieces when loading a previous saved game
            // Also restores the colors of the graveyard pieces to match the colors on the chessboard
            this.lightColor = (Color) in.readObject();
            this.darkColor = (Color) in.readObject();
            this.whitePieceColor = (Color) in.readObject();
            this.blackPieceColor = (Color) in.readObject();

            this.state = loaded;
            boardPanel.setState(loaded);

            historyModel.clear();
            for(String s:hist) historyModel.addElement(s);

            updateGraveyards();
            boardPanel.repaint();

        } catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load: "+ex);
        }
    }
});

        newGameBtn = new JButton("New Game");
        newGameBtn.addActionListener(e -> {
            this.state = new GameState();
            boardPanel.setState(this.state);
            historyModel.clear();
            updateGraveyards();
            boardPanel.repaint();
        });

        gameSettingsPanel.add(saveBtn); gameSettingsPanel.add(loadBtn); gameSettingsPanel.add(newGameBtn);
        topControls.add(gameSettingsPanel);

        // The graveyards are used to store pieces that have been captured by the other player
        // This is a way to keep track of what pieces remain on the board as well as what pieces are absent
        // This Jpanel stores the corresponding pieces to their respective opponent
        // eg. The black player captures a white knight and is stored in the white graveyard
         JPanel graves = new JPanel(new GridLayout(2,1));
        whiteGrave = new JPanel(); whiteGrave.setBorder(BorderFactory.createTitledBorder("White Captured"));
        whiteGrave.setLayout(new FlowLayout(FlowLayout.LEFT));
        blackGrave = new JPanel(); blackGrave.setBorder(BorderFactory.createTitledBorder("Black Captured"));
        blackGrave.setLayout(new FlowLayout(FlowLayout.LEFT));
        graves.add(whiteGrave); graves.add(blackGrave);

        right.add(topControls, BorderLayout.NORTH);
        right.add(graves, BorderLayout.SOUTH);

        updateGraveyards();

        topControls.revalidate();
        topControls.repaint();

        JPanel left = new JPanel(new BorderLayout());
        left.add(boardPanel, BorderLayout.CENTER);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(left, BorderLayout.CENTER);
        getContentPane().add(right, BorderLayout.EAST);

        pack();
        setMinimumSize(getSize());
    }

    // This function makes sure the pieces are listed in the graveyard in the corresponding order in which they were captured
    void addHistory(String s) {
        historyModel.addElement(s);
        historyList.ensureIndexIsVisible(historyModel.size()-1);
    }
    // This function updates the graveyards with every piece captured in a chess game
    void updateGraveyards() {
        whiteGrave.removeAll();
        blackGrave.removeAll();
        adjustGraveyardPanel(whiteGrave, state.capturedByBlack, whitePieceColor);
        adjustGraveyardPanel(blackGrave, state.capturedByWhite, blackPieceColor);
        revalidate();
        repaint();
    }

    // This function adjusts the size of the graveyard panel with respect to the size of the board
    // This was previously defined in the sizeCombo function which dynamically changes the size of the board
    void adjustGraveyardPanel(JPanel panel, java.util.List<Piece> list, Color pieceColor) {
        panel.removeAll();
        int n = list.size();
        if (n == 0) return;
        int panelHeight = panel.getHeight() > 0 ? panel.getHeight() : 100;
        int fontSize = Math.min(36, Math.max(10, panelHeight / Math.max(1, n)));
        for (Piece p : list) {
            JLabel lbl = new JLabel(p.unicode());
            lbl.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fontSize));
            lbl.setForeground(p.color == Piece.Color.WHITE ? whitePieceColor : blackPieceColor);
            panel.add(lbl);
        }
        panel.revalidate();
        panel.repaint();
    }

// This function checks what is currently going on in the chess game
void checkGameState() {
    Piece.Color current = state.turn; // player who is about to move
    boolean inCheck = MoveGenerator.isKingInCheck(state, current);

    // Generate all legal moves for current player
    java.util.List<Move> legalMoves = MoveGenerator.generateLegalMoves(state);

    // This shows the current state of the king relative to what moves are possible
    if (inCheck && legalMoves.isEmpty()) {

        // This if statement validates that no possible moves can happen
        // And therefore the opponent wins because the king in unable to be saved by itself or other pieces
        String winner = (current == Piece.Color.WHITE) ? "Black" : "White";
        JOptionPane.showMessageDialog(this, "Checkmate! " + winner + " wins!");

    } else if (!inCheck && legalMoves.isEmpty()) {

       // This else if statement proposes that the king is not in check
       // And no moves are possible which means both kings are safe and its a tie
        JOptionPane.showMessageDialog(this, "Stalemate! It's a draw.");

    } else if (inCheck) {

        // This else if statement means that the king is in check but moves are possible
        JOptionPane.showMessageDialog(this, current + " is in check!");
    }
}

}

// This class is focused on the user interactions within the chess game
// That includes move movements such as clicking the mouse, dragging the mouse, and releasing the mouse on commands
class BoardPanel extends JPanel {
    GameState state;
    ChessFrame parent;
    int squareSize = 70;
    Point dragFrom = null;
    Piece draggingPiece = null;
    Point mousePos = null;

    public BoardPanel(GameState s, ChessFrame parent) {
        this.state = s;
        this.parent = parent;
        setPreferredSize(new Dimension(8 * squareSize, 8 * squareSize));
        setBorder(new LineBorder(Color.DARK_GRAY));

// This function focuses on the dragging of the piece to specific squares on the chessboard
// It also focuses on it being a valid placement of the piece when you press on that piece
        MouseAdapter ma = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int c = e.getX() / squareSize;
                int r = 7 - (e.getY() / squareSize);
                if (r < 0 || r > 7 || c < 0 || c > 7) return;
                Piece p = state.board[r][c];
                if (p != null && p.color == state.turn) {
                    dragFrom = new Point(r, c);
                    draggingPiece = p;
                    mousePos = e.getPoint();
                }
            }
            // This function locates where the mouse dragged the piece from in terms of position
            public void mouseDragged(MouseEvent e) {
                if (draggingPiece != null) {
                    mousePos = e.getPoint();
                    repaint();
                }
            }

            // This function is when you confirm your move in a chess game and move on to the opponents turn
            public void mouseReleased(MouseEvent e) {
                if (draggingPiece != null && dragFrom != null) {
                    int tc = e.getX() / squareSize;
                    int tr = 7 - (e.getY() / squareSize);
                    if (tr < 0 || tr > 7 || tc < 0 || tc > 7) {
                        draggingPiece = null;
                        dragFrom = null;
                        repaint();
                        return;
                    }

                    Move mv = new Move(dragFrom.x, dragFrom.y, tr, tc);
                    java.util.List<Move> legal = MoveGenerator.generateLegalMoves(state);
                    boolean found = false;
                    Move chosen = null;
                    for (Move m : legal) {
                        if (m.equalCoordinates(mv)) {
                            found = true;
                            chosen = m;
                            break;
                        }
                    }
                    // This if statement is to see if a valid move was found and applied
                    if (found) {
                        state.applyMove(chosen);
                        parent.addHistory(chosen.toNotation());
                        parent.updateGraveyards();
                        repaint();
                        parent.checkGameState(); // <-- check for check/checkmate/stalemate
                    }
                }

                draggingPiece = null;
                dragFrom = null;
                mousePos = null;
                repaint();
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

// This setter sets the size of the each square on a chess board
    void setSquareSize(int s) {
        this.squareSize = s;
        setPreferredSize(new Dimension(8 * squareSize, 8 * squareSize));
    }

// This setter sets the state of the game
    void setState(GameState s) {
        this.state = s;
    }

// This function paints the individual squares on a chessboard with respect to the color of the player
    public void paintComponent(Graphics g0) {
    super.paintComponent(g0);
    Graphics2D g = (Graphics2D) g0;
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    int size = squareSize;

    // This loop statement draws all 64 squares (8x8) either light or dark
    for (int r = 0; r < 8; r++) {
        for (int c = 0; c < 8; c++) {
            int x = c * size;
            int y = (7 - r) * size;
            boolean light = ((r + c) % 2 == 0);
            g.setColor(light ? parent.lightColor : parent.darkColor);
            g.fillRect(x, y, size, size);
        }
    }

    // This loop statement finds where the king is and highlights it red when it's in danger
    for (int r = 0; r < 8; r++) {
        for (int c = 0; c < 8; c++) {
            Piece p = state.board[r][c];
            if (p != null && p.type == Piece.Type.KING) {
                boolean inCheck = MoveGenerator.isKingInCheck(state, p.color);
                if (inCheck) {
                    g.setColor(new Color(255, 0, 0, 150)); // semi-transparent red
                    g.fillRect(c * size, (7 - r) * size, size, size);
                }
            }
        }
    }

    // This draws the individual coordinates on the side of the game from 1-8 and A-H
    g.setColor(Color.DARK_GRAY);
    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(10, size / 6)));
    for (int i = 0; i < 8; i++) {
        g.drawString("" + (char) ('A' + i), i * size + 4, getHeight() - 4);
        g.drawString("" + (8 - i), 2, i * size + size / 6 + 12);
    }

    // This draws the pieces in Unicode on the chessboard itself when you launch the GUI 
    for (int r = 0; r < 8; r++) {
        for (int c = 0; c < 8; c++) {
            Piece p = state.board[r][c];
            if (p == null) continue;
            if (dragFrom != null && dragFrom.x == r && dragFrom.y == c && draggingPiece != null)
                continue; // skip dragging piece
            drawPiece(g, p, c * size, (7 - r) * size, size);
        }
    }

    // Draw dragging piece on top
    if (draggingPiece != null && mousePos != null) {
        int px = mousePos.x - size / 2;
        int py = mousePos.y - size / 2;
        drawPiece(g, draggingPiece, px, py, size);
    }
}

    // This function draws the pieces on the chessboard in unicode
    // Also includes the dimensions to fit the squares nicely
    void drawPiece(Graphics2D g, Piece p, int x, int y, int size) {
        String s = p.unicode();
        int fontSize = (int) (size * 0.8);
        g.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        FontMetrics fm = g.getFontMetrics();
        int sw = fm.stringWidth(s);
        int sh = fm.getAscent();
        Color pc = (p.color == Piece.Color.WHITE) ? parent.whitePieceColor : parent.blackPieceColor;
        g.setColor(pc);
        g.drawString(s, x + (size - sw) / 2, y + (size + sh) / 2 - (size / 10));
    }
}


// Game state, pieces, move stack
class GameState implements Serializable {
    Piece[][] board = new Piece[8][8];
    Piece.Color turn = Piece.Color.WHITE;
    Stack<Move> history = new Stack<>();
    java.util.List<Piece> capturedByWhite = new ArrayList<>();
    java.util.List<Piece> capturedByBlack = new ArrayList<>();
    // en passant target square (row,col) for the next move
    transient int[] enPassantTarget = null; // not serialized; we'll store in moves

    public GameState() { initStandard(); }

    // This function initalizes where the pieces appear on the chess board
    void initStandard() {
        // These loop statements place the pawns on the board from position 1 to 8
        for (int c=0;c<8;c++) board[1][c] = new Piece(Piece.Type.PAWN, Piece.Color.WHITE);
        for (int c=0;c<8;c++) board[6][c] = new Piece(Piece.Type.PAWN, Piece.Color.BLACK);

        // These statements are the coordinates for where the rooks will be placed on the chess board
        board[0][0] = new Piece(Piece.Type.ROOK, Piece.Color.WHITE);
        board[0][7] = new Piece(Piece.Type.ROOK, Piece.Color.WHITE);
        board[7][0] = new Piece(Piece.Type.ROOK, Piece.Color.BLACK);
        board[7][7] = new Piece(Piece.Type.ROOK, Piece.Color.BLACK);
        
        // These statements are the coordinates for where the knights will be placed on the chess board
        board[0][1] = new Piece(Piece.Type.KNIGHT, Piece.Color.WHITE);
        board[0][6] = new Piece(Piece.Type.KNIGHT, Piece.Color.WHITE);
        board[7][1] = new Piece(Piece.Type.KNIGHT, Piece.Color.BLACK);
        board[7][6] = new Piece(Piece.Type.KNIGHT, Piece.Color.BLACK);
        
        // These statements are the coordinates for where the bishops will be placed on the chess board
        board[0][2] = new Piece(Piece.Type.BISHOP, Piece.Color.WHITE);
        board[0][5] = new Piece(Piece.Type.BISHOP, Piece.Color.WHITE);
        board[7][2] = new Piece(Piece.Type.BISHOP, Piece.Color.BLACK);
        board[7][5] = new Piece(Piece.Type.BISHOP, Piece.Color.BLACK);

        // These statements are the coordinates for where the queens will be placed on the chess board
        board[0][3] = new Piece(Piece.Type.QUEEN, Piece.Color.WHITE);
        board[7][3] = new Piece(Piece.Type.QUEEN, Piece.Color.BLACK);

        // These statements are the coordinates for where the kings will be placed on the chess board
        board[0][4] = new Piece(Piece.Type.KING, Piece.Color.WHITE);
        board[7][4] = new Piece(Piece.Type.KING, Piece.Color.BLACK);
    }

    // This function is used to show if a king exists on either side of the chess board
    boolean hasKing(Piece.Color c) {
        for (int r=0;r<8;r++) for (int cc=0;cc<8;cc++) if (board[r][cc]!=null && board[r][cc].type==Piece.Type.KING && board[r][cc].color==c) return true;
        return false;
    }

    void applyMove(Move m) {
        // apply and push into history
        Piece moving = board[m.fromR][m.fromC];
        Piece captured = board[m.toR][m.toC];
        // handle en passant capture
        if (m.enPassant) {
            int capR = m.fromR; // pawn moved two? Actually enPassant means capturing pawn behind
            int capC = m.toC;
            captured = board[capR][capC];
            board[capR][capC] = null;
            if (captured!=null) addToCapturedList(captured);
        } else if (captured!=null) {
            addToCapturedList(captured);
        }

        // handle castling
        if (m.castling) {

            // king moved two squares
            if (m.toC==6) { // king side
                board[m.toR][5] = board[m.toR][7];
                board[m.toR][7] = null;
                if (board[m.toR][5]!=null) board[m.toR][5].hasMoved=true;

            } else if (m.toC==2) { // queen side
                board[m.toR][3] = board[m.toR][0];
                board[m.toR][0] = null;
                if (board[m.toR][3]!=null) board[m.toR][3].hasMoved=true;
            }
        }
        board[m.toR][m.toC] = moving;
        board[m.fromR][m.fromC] = null;
        if (m.promotion!=null) {
            board[m.toR][m.toC] = new Piece(m.promotion, moving.color);
        }
        if (moving!=null) moving.hasMoved=true;
        history.push(m);

        // set en passant target: if pawn moved two squares
        if (moving!=null && moving.type==Piece.Type.PAWN && Math.abs(m.toR - m.fromR)==2) {
            m.enPassantTarget = new int[]{ (m.fromR + m.toR)/2, m.fromC };
        }
        // clear enPassant on previous moves already tied to moves
        turn = (turn==Piece.Color.WHITE)? Piece.Color.BLACK: Piece.Color.WHITE;
    }

    // This function adds captured pieces to a list to later be represented in the graveyards
    void addToCapturedList(Piece p) {
        if (p.color==Piece.Color.WHITE) capturedByBlack.add(p);
        else capturedByWhite.add(p);
    }

    // This function is for the undo button and reverts the move to the previous states
    boolean undoLast() {
        if (history.isEmpty()) return false;
        Move m = history.pop();
        Piece moving = board[m.toR][m.toC];
        board[m.fromR][m.fromC] = moving;
        board[m.toR][m.toC] = null;
        if (m.promotion!=null) { // revert promotion to pawn
            board[m.fromR][m.fromC] = new Piece(Piece.Type.PAWN, moving.color);
        }
        moving.hasMoved = m.movedBefore;
        // restore captured
        if (m.captured!=null) {
            if (m.enPassant) {
                board[m.fromR][m.toC] = m.captured; // restore captured pawn behind
            } else {
                board[m.toR][m.toC] = m.captured;
            }
            // remove from captured lists
            if (m.captured.color==Piece.Color.WHITE) capturedByBlack.remove(capturedByBlack.size()-1);
            else capturedByWhite.remove(capturedByWhite.size()-1);
        }
        // revert castling rooks
        if (m.castling) {
            if (m.toC==6) { // king side
                board[m.toR][7] = board[m.toR][5]; board[m.toR][5]=null;
            } else if (m.toC==2) { // queen side
                board[m.toR][0] = board[m.toR][3]; board[m.toR][3]=null;
            }
        }
        turn = (turn==Piece.Color.WHITE)? Piece.Color.BLACK: Piece.Color.WHITE;
        return true;
    }
}

// This class shows how the pieces are generated on the chessboard
class Piece implements Serializable {
    enum Type { KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN }
    enum Color { WHITE, BLACK }
    Type type; Color color; boolean hasMoved=false; boolean movedBefore=false;
    public Piece(Type type, Color color) { this.type=type; this.color=color; }
    String unicode(){
        // Use black/white chess unicode glyphs to represent the pieces on the board
        switch(type) {
            case KING: return color==Color.WHITE? "\u2654" : "\u265A";
            case QUEEN: return color==Color.WHITE? "\u2655" : "\u265B";
            case ROOK: return color==Color.WHITE? "\u2656" : "\u265C";
            case BISHOP: return color==Color.WHITE? "\u2657" : "\u265D";
            case KNIGHT: return color==Color.WHITE? "\u2658" : "\u265E";
            case PAWN: return color==Color.WHITE? "\u2659" : "\u265F";
        }
        return "?";
    }
}

// This class uses the different pieces and how they interact when moved
class Move implements Serializable {
    int fromR, fromC, toR, toC;
    boolean castling=false, enPassant=false;
    Piece captured = null;
    Piece.Type promotion = null;
    boolean movedBefore=false;
    int[] enPassantTarget = null;

    public Move(int fr,int fc,int tr,int tc){ fromR=fr; fromC=fc; toR=tr; toC=tc; }

    boolean equalCoordinates(Move o) { return fromR==o.fromR && fromC==o.fromC && toR==o.toR && toC==o.toC; }

    // This notation is used in the Move History when an additional non standard rule is applied
    String toNotation(){
        String from = coordToString(fromR, fromC);
        String to = coordToString(toR, toC);
        String mid = enPassant? " (enPassant) " : castling? " (castling)" : "";
        String prom = promotion!=null? "="+promotion.name().charAt(0):"";
        return from+" -> "+to+prom+mid;
    }

    static String coordToString(int r,int c){
        char file = (char)('A'+c);
        char rank = (char)('1'+r);
        return ""+file+rank;
    }
}

// This class is used to generate all possible moves by either player at every given opportunity
// This class includes
// En passant, pawn promotion, and castling rules
// Illustrates the possibilities for a King 
// Shows the way that each piece moves on a chess board
// Makes sure to validate each move in the chess game bounds
class MoveGenerator {

    // Generates all legal moves for the current player
    static java.util.List<Move> generateLegalMoves(GameState s) {
        java.util.List<Move> moves = generatePseudoLegal(s);
        java.util.List<Move> legal = new ArrayList<>();
        for (Move m : moves) {
            GameState copy = deepCopy(s);
            applyMoveOnCopy(copy, m);
            // Only add move if king is not left in check
            if (!isKingInCheck(copy, s.turn)) legal.add(m);
        }
        return legal;
    }

    // Apply a move on a copy of the game state (for legality checking)
    static void applyMoveOnCopy(GameState copy, Move m) {
        Piece moving = copy.board[m.fromR][m.fromC];
        if (moving == null) return;

        // This handles all normal standard rule captures
        Piece captured = copy.board[m.toR][m.toC];
        if (captured != null) {
            m.captured = captured;
            copy.addToCapturedList(captured);
            copy.board[m.toR][m.toC] = null;
        }

        // This handles the en passant rule of pawns attacking horizontally
        if (m.enPassant) {
            int capR = m.fromR;
            int capC = m.toC;
            if (copy.board[capR][capC] != null) {
                m.captured = copy.board[capR][capC];
                copy.addToCapturedList(m.captured);
                copy.board[capR][capC] = null;
            }
        }

        // This demonstrates castling
        // Whether it's a king & rook castling on the right side
        // Whether it's a queen & rook castling on the left side
        if (m.castling) {
            if (m.toC == 6) { // king side
                copy.board[m.toR][5] = copy.board[m.toR][7];
                copy.board[m.toR][7] = null;
            } else if (m.toC == 2) { // queen side
                copy.board[m.toR][3] = copy.board[m.toR][0];
                copy.board[m.toR][0] = null;
            }
        }

        // Move the piece
        copy.board[m.toR][m.toC] = moving;
        copy.board[m.fromR][m.fromC] = null;

        // Switch turn
        copy.turn = (copy.turn == Piece.Color.WHITE) ? Piece.Color.BLACK : Piece.Color.WHITE;
    }

    // Check if a king of given color is in check
    static boolean isKingInCheck(GameState s, Piece.Color color) {
        int kr = -1, kc = -1;
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            Piece p = s.board[r][c];
            if (p != null && p.type == Piece.Type.KING && p.color == color) {
                kr = r; kc = c;
            }
        }
        if (kr == -1) return true; // King missing => captured => check

        // Generate all opponent attack moves
        Piece.Color opp = (color == Piece.Color.WHITE) ? Piece.Color.BLACK : Piece.Color.WHITE;
        java.util.List<Move> attacks = generatePseudoLegal(s, opp, true);
        for (Move a : attacks) if (a.toR == kr && a.toC == kc) return true;
        return false;
    }

    // This shows how each piece is able to move and how it's logical to the game of chess
    static java.util.List<Move> generatePseudoLegal(GameState s) { return generatePseudoLegal(s, s.turn, false); }
    static java.util.List<Move> generatePseudoLegal(GameState s, Piece.Color color, boolean onlyAttacks) {
        java.util.List<Move> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            Piece p = s.board[r][c]; if (p == null || p.color != color) continue;
            switch (p.type) {
                case PAWN: generatePawnMoves(s,r,c,moves,onlyAttacks); break;
                case KNIGHT: generateKnightMoves(s,r,c,moves); break;
                case BISHOP: generateSliding(s,r,c,moves,new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}}); break;
                case ROOK: generateSliding(s,r,c,moves,new int[][]{{1,0},{-1,0},{0,1},{0,-1}}); break;
                case QUEEN: generateSliding(s,r,c,moves,new int[][]{{1,1},{1,-1},{-1,1},{-1,-1},{1,0},{-1,0},{0,1},{0,-1}}); break;
                case KING: generateKingMoves(s,r,c,moves); break;
            }
        }
        return moves;
    }

    // This function is used with the game state to determine whe a pawn is able to attack a piece
    static void generatePawnMoves(GameState s, int r, int c, java.util.List<Move> moves, boolean onlyAttacks) {
        Piece p = s.board[r][c]; int dir = (p.color==Piece.Color.WHITE? 1 : -1);
        int start = (p.color==Piece.Color.WHITE? 1 : 6);
        int nextR = r + dir;

        // These if statements show that the pawn must be in bounds and next to a piece of the opposing color
        if (!onlyAttacks) {
            if (inBounds(nextR,c) && s.board[nextR][c]==null) {
                Move m = new Move(r,c,nextR,c);
                if ((p.color==Piece.Color.WHITE && nextR==7)||(p.color==Piece.Color.BLACK && nextR==0)) m.promotion=Piece.Type.QUEEN;
                moves.add(m);
                if (r==start) {
                    int twoR = r+2*dir;
                    if (inBounds(twoR,c) && s.board[twoR][c]==null) moves.add(new Move(r,c,twoR,c));
                }
            }
        }

        // Pawn can either capture diagonally or horizontally via en passant
        for (int dc=-1; dc<=1; dc+=2) {
            int cc = c+dc, rr = r+dir;
            if (!inBounds(rr,cc)) continue;
            if (s.board[rr][cc]!=null && s.board[rr][cc].color!=p.color) moves.add(new Move(r,c,rr,cc));
            else if (onlyAttacks) moves.add(new Move(r,c,rr,cc));

            // en passant is a rule where the pawn can capture horizontally and still move diagonally
            if (!s.history.isEmpty()) {
                Move lm = s.history.peek();
                if (Math.abs(lm.toR-lm.fromR)==2 && lm.toR==r && Math.abs(lm.toC-c)==1) {
                    Move m = new Move(r,c,r+dir,lm.toC); m.enPassant=true; moves.add(m);
                }
            }
        }
    }

    // This function uses coordinates to generate the L shape and jumping pattern that the knight can do
    static void generateKnightMoves(GameState s, int r,int c, java.util.List<Move> moves) {

        // These coordinates eg. {2,1} means that a knight will move 2 units right and up 1 unit 
        // like a vector
        int[][] deltas = {{2,1},{2,-1},{-2,1},{-2,-1},{1,2},{1,-2},{-1,2},{-1,-2}};
        Piece p = s.board[r][c];
        for (int[] d: deltas) {
            int rr=r+d[0], cc=c+d[1]; if (!inBounds(rr,cc)) continue;
            if (s.board[rr][cc]==null || s.board[rr][cc].color!=p.color) moves.add(new Move(r,c,rr,cc));
        }
    }

    // This function applies to pieces like the queen, rook, bishop 
    // which can move "infinitely" in a particular direction (X format for bishop)
    // + shape for rook, and the queen has a combination of both an X and + movement pattern *
    static void generateSliding(GameState s,int r,int c,java.util.List<Move> moves,int[][] dirs){
        Piece p = s.board[r][c];
        for (int[] d: dirs) {
            int rr=r+d[0], cc=c+d[1];
            while (inBounds(rr,cc)) {
                if (s.board[rr][cc]==null) moves.add(new Move(r,c,rr,cc));
                else { if (s.board[rr][cc].color!=p.color) moves.add(new Move(r,c,rr,cc)); break; }
                rr+=d[0]; cc+=d[1];
            }
        }
    }
    
    // This function represents the moves in which a king is able to do
    static void generateKingMoves(GameState s,int r,int c,java.util.List<Move> moves){
        Piece p = s.board[r][c];
        for(int dr=-1;dr<=1;dr++) for(int dc=-1;dc<=1;dc++){
            if(dr==0 && dc==0) continue; int rr=r+dr, cc=c+dc; if(!inBounds(rr,cc)) continue;
            if(s.board[rr][cc]==null || s.board[rr][cc].color!=p.color) moves.add(new Move(r,c,rr,cc));
        }
        if(!p.hasMoved){
            // King-side castling where the rook and king switch in a sense. Better protection for king
            if(inBounds(r,7) && s.board[r][7]!=null && s.board[r][7].type==Piece.Type.ROOK && !s.board[r][7].hasMoved)
                if(s.board[r][5]==null && s.board[r][6]==null) moves.add(new Move(r,c,r,6){ { castling=true; } });

            // Queen-side castling where the rook and queen switch in a sense, easier to move the queen out for offense
            if(inBounds(r,0) && s.board[r][0]!=null && s.board[r][0].type==Piece.Type.ROOK && !s.board[r][0].hasMoved)
                if(s.board[r][1]==null && s.board[r][2]==null && s.board[r][3]==null) moves.add(new Move(r,c,r,2){ { castling=true; } });
        }
    }

    // This static is to represent all moves applied
    // and if they're out of the correct coordinates, the move is not in bounds
    static boolean inBounds(int r,int c){ return r>=0 && r<8 && c>=0 && c<8; }

    // Deep copy for move legality checking
    static GameState deepCopy(GameState s) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(s);
            oos.flush();
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (GameState) ois.readObject();
        } catch (Exception ex) { ex.printStackTrace(); return null; }
    }
}
