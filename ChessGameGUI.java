import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

// enums to describe the pieces on the board in terms of type, color, and size
enum PieceType { KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN }
enum PieceColor { WHITE, BLACK }
enum BoardSize { SMALL, MEDIUM, LARGE }

// class to implement unicode which codes images to show up on the GUI
class Piece implements Serializable {
    PieceType type;
    PieceColor color;
    String unicode;

// Defines the piece in unicode on the board
    public Piece(PieceType type, PieceColor color) {
        this.type = type;
        this.color = color;
        this.unicode = getUnicode(type, color);
    }

    private String getUnicode(PieceType type, PieceColor color) {
        switch (type) {
            case KING: return color == PieceColor.WHITE ? "\u2654" : "\u265A";
            case QUEEN: return color == PieceColor.WHITE ? "\u2655" : "\u265B";
            case ROOK: return color == PieceColor.WHITE ? "\u2656" : "\u265C";
            case BISHOP: return color == PieceColor.WHITE ? "\u2657" : "\u265D";
            case KNIGHT: return color == PieceColor.WHITE ? "\u2658" : "\u265E";
            case PAWN: return color == PieceColor.WHITE ? "\u2659" : "\u265F";
        }
        return "";
    }

    @Override
    public String toString() { return unicode; }
}

// This class defines the movement of pieces (No chess rule restrictions implemented yet so pieces can move wherever)
class Move implements Serializable {
    Point from, to;
    Piece movedPiece, capturedPiece;

    public Move(Point from, Point to, Piece movedPiece, Piece capturedPiece) {
        this.from = from; this.to = to;
        this.movedPiece = movedPiece; this.capturedPiece = capturedPiece;
    }

// Class that defines the coordinates of pieces (A1 - H8)
    private String toChessNotation(Point p) {
        char file = (char) ('A' + p.y);
        int rank = 8 - p.x;
        return "" + file + rank;
    }

    @Override
    public String toString() {
        String str = movedPiece + ": " + toChessNotation(from) + " -> " + toChessNotation(to);
        if (capturedPiece != null) str += " (captured " + capturedPiece + ")";
        return str;
    }
}

// This class demonstrates the body of what the chess board looks like and the pieces associated with it 
public class ChessGameGUI extends JFrame {
    private JPanel boardPanel;
    private JButton[][] squares = new JButton[8][8];
    private Map<Point, Piece> boardMap = new HashMap<>();
    private Stack<Move> moveHistory = new Stack<>();

    private JLabel dragPieceLabel = new JLabel("", SwingConstants.CENTER);
    private Point dragStart = null;
    private Piece draggingPiece = null;

    private Color lightColor = new Color(240, 217, 181);
    private Color darkColor = new Color(181, 136, 99);
    private Color whitePieceColor = Color.WHITE;
    private Color blackPieceColor = Color.BLACK;

    private BoardSize boardSize = BoardSize.MEDIUM;

    private JTextArea moveHistoryArea = new JTextArea();
    private JPanel whiteCapturedPanel = new JPanel(new GridLayout(2, 8, 5, 5));
    private JPanel blackCapturedPanel = new JPanel(new GridLayout(2, 8, 5, 5));

// class method to set the chess board up to be playable
    public ChessGameGUI() {
        setTitle("Chess Game GUI Design");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        createMenuBar();
        initializeBoard();
        initializePieces();
        add(boardPanel, BorderLayout.CENTER);
        createHistoryPanel();
        updateBoardSize();
        setSize(1000, 800);
        setVisible(true);
    }
    // This function initializes the board before the first move is made
    private void initializeBoard() {
        boardPanel = new JPanel(new GridLayout(8, 8));
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                JButton square = new JButton();
                square.setOpaque(true);
                square.setBorderPainted(false);
                square.setBackground((row + col) % 2 == 0 ? lightColor : darkColor);
                final int r = row, c = col;

                // This function refers to picking up and dragging a piece during a move
                square.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        Point p = new Point(r, c);
                        Piece piece = boardMap.get(p);
                        if (piece != null) {
                            draggingPiece = piece;
                            dragStart = p;
                            dragPieceLabel.setText(piece.toString());
                            dragPieceLabel.setFont(new Font("SansSerif", Font.PLAIN, getPieceFontSize()));
                            dragPieceLabel.setForeground(piece.color == PieceColor.WHITE ? whitePieceColor : blackPieceColor);
                            dragPieceLabel.setSize(square.getWidth(), square.getHeight());
                            dragPieceLabel.setLocation(e.getXOnScreen() - square.getWidth()/2,
                                                       e.getYOnScreen() - square.getHeight()/2);
                            getLayeredPane().add(dragPieceLabel, JLayeredPane.DRAG_LAYER);
                            square.setText("");
                        }
                    }
                    // This function refers to when the user releases the piece and therefore dropping it into a location
                    public void mouseReleased(MouseEvent e) {
                    if (draggingPiece != null && dragStart != null) {
                        Point target = getSquareFromCoordinates(e.getXOnScreen(), e.getYOnScreen());
                        if (target != null) {
                            Piece captured = boardMap.get(target);
                            removePiece(target.x, target.y);
                            setPiece(target.x, target.y, draggingPiece);

                                if (captured != null) addCapturedPiece(captured);

                                moveHistory.push(new Move(dragStart, target, draggingPiece, captured));
                                updateMoveHistory();

                                if (captured != null && captured.type == PieceType.KING) {
                                    JOptionPane.showMessageDialog(ChessGameGUI.this,
                                            captured.color == PieceColor.WHITE ? "Black wins!" : "White wins!");
                                    System.exit(0);
                                }
                            } else {
                                setPiece(dragStart.x, dragStart.y, draggingPiece);
                            }
                            getLayeredPane().remove(dragPieceLabel);
                            getLayeredPane().repaint();
                            draggingPiece = null;
                            dragStart = null;
                        }
                    }
                });
                // This function focuses on how the piece looks while it is being dragged
                square.addMouseMotionListener(new MouseMotionAdapter() {
                    public void mouseDragged(MouseEvent e) {
                        if (draggingPiece != null) {
                            dragPieceLabel.setLocation(e.getXOnScreen() - dragPieceLabel.getWidth()/2,
                                                       e.getYOnScreen() - dragPieceLabel.getHeight()/2);
                        }
                    }
                });

                squares[row][col] = square;
                boardPanel.add(square);
            }
        }
    }
    // This function initializes the pieces to the correct coordinate locations before the chess game starts
    private void initializePieces() {
        boardMap.clear();
        whiteCapturedPanel.removeAll();
        blackCapturedPanel.removeAll();

        for (int i = 0; i < 8; i++) setPiece(6, i, new Piece(PieceType.PAWN, PieceColor.WHITE));
        setPiece(7, 0, new Piece(PieceType.ROOK, PieceColor.WHITE));
        setPiece(7, 1, new Piece(PieceType.KNIGHT, PieceColor.WHITE));
        setPiece(7, 2, new Piece(PieceType.BISHOP, PieceColor.WHITE));
        setPiece(7, 3, new Piece(PieceType.QUEEN, PieceColor.WHITE));
        setPiece(7, 4, new Piece(PieceType.KING, PieceColor.WHITE));
        setPiece(7, 5, new Piece(PieceType.BISHOP, PieceColor.WHITE));
        setPiece(7, 6, new Piece(PieceType.KNIGHT, PieceColor.WHITE));
        setPiece(7, 7, new Piece(PieceType.ROOK, PieceColor.WHITE));

        for (int i = 0; i < 8; i++) setPiece(1, i, new Piece(PieceType.PAWN, PieceColor.BLACK));
        setPiece(0, 0, new Piece(PieceType.ROOK, PieceColor.BLACK));
        setPiece(0, 1, new Piece(PieceType.KNIGHT, PieceColor.BLACK));
        setPiece(0, 2, new Piece(PieceType.BISHOP, PieceColor.BLACK));
        setPiece(0, 3, new Piece(PieceType.QUEEN, PieceColor.BLACK));
        setPiece(0, 4, new Piece(PieceType.KING, PieceColor.BLACK));
        setPiece(0, 5, new Piece(PieceType.BISHOP, PieceColor.BLACK));
        setPiece(0, 6, new Piece(PieceType.KNIGHT, PieceColor.BLACK));
        setPiece(0, 7, new Piece(PieceType.ROOK, PieceColor.BLACK));
    }
    // This function sets the color for the pieces
    private void setPiece(int row, int col, Piece piece) {
        boardMap.put(new Point(row, col), piece);
        squares[row][col].setText(piece.toString());
        squares[row][col].setForeground(piece.color == PieceColor.WHITE ? whitePieceColor : blackPieceColor);
    }
    // This function removes a piece when it is captured
    private void removePiece(int row, int col) {
        boardMap.remove(new Point(row, col));
        squares[row][col].setText("");
    }

    // This function gets the coordinates for each square on the board
    private Point getSquareFromCoordinates(int xScreen, int yScreen) {
        Point boardLoc = boardPanel.getLocationOnScreen();
        int x = xScreen - boardLoc.x;
        int y = yScreen - boardLoc.y;
        int col = x / (boardPanel.getWidth() / 8);
        int row = y / (boardPanel.getHeight() / 8);
        if (row >= 0 && row < 8 && col >= 0 && col < 8) return new Point(row, col);
        return null;
    }

    // This function implements the menu bar which is shown in the top left corner of the implemented chess game GUI
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");
        JMenuItem newGame = new JMenuItem("New Game");
        newGame.addActionListener(e -> { initializePieces(); moveHistory.clear(); moveHistoryArea.setText(""); });
        JMenuItem saveGame = new JMenuItem("Save Game");
        saveGame.addActionListener(e -> saveGame());
        JMenuItem loadGame = new JMenuItem("Load Game");
        loadGame.addActionListener(e -> loadGame());
        gameMenu.add(newGame); gameMenu.add(saveGame); gameMenu.add(loadGame);

        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem customize = new JMenuItem("Customize Board/ Pieces");
        customize.addActionListener(e -> new SettingsWindow(this));
        settingsMenu.add(customize);

        menuBar.add(gameMenu); menuBar.add(settingsMenu);
        setJMenuBar(menuBar);
    }

    // This function allows for save files to be created and loaded later
    private void saveGame() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("chessgame.dat"))) {
            oos.writeObject(boardMap);
            oos.writeObject(moveHistory);
            JOptionPane.showMessageDialog(this, "Game saved!");
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    @SuppressWarnings("unchecked")

    // This function loads a previous saved chess game that has already happened, or loads nothing if no save file
    private void loadGame() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("chessgame.dat"))) {
            boardMap = (Map<Point, Piece>) ois.readObject();
            moveHistory = (Stack<Move>) ois.readObject();
            for (int row = 0; row < 8; row++)
                for (int col = 0; col < 8; col++)
                    squares[row][col].setText("");
            for (Map.Entry<Point, Piece> entry : boardMap.entrySet())
                setPiece(entry.getKey().x, entry.getKey().y, entry.getValue());
            updateMoveHistory();
            JOptionPane.showMessageDialog(this, "Game loaded!");
        } catch (IOException | ClassNotFoundException ex) { ex.printStackTrace(); }
    }
    
    // This function shows the box where the move history is constructed line by line
    private void createHistoryPanel() {
        JPanel historyPanel = new JPanel(new BorderLayout());

        whiteCapturedPanel.setBorder(BorderFactory.createTitledBorder("White Captured"));
        blackCapturedPanel.setBorder(BorderFactory.createTitledBorder("Black Captured"));

        JScrollPane whiteScroll = new JScrollPane(whiteCapturedPanel);
        JScrollPane blackScroll = new JScrollPane(blackCapturedPanel);
        JPanel capturedPanels = new JPanel(new GridLayout(2,1));
        capturedPanels.add(whiteScroll);
        capturedPanels.add(blackScroll);

        historyPanel.add(capturedPanels, BorderLayout.NORTH);

        moveHistoryArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(moveHistoryArea);
        historyPanel.add(scrollPane, BorderLayout.CENTER);

        JButton undoBtn = new JButton("Undo");
        undoBtn.addActionListener(e -> {
            if (!moveHistory.isEmpty()) {
                Move last = moveHistory.pop();
                removePiece(last.to.x, last.to.y);
                setPiece(last.from.x, last.from.y, last.movedPiece);
                if (last.capturedPiece != null) {
                    setPiece(last.to.x, last.to.y, last.capturedPiece);
                    removeCapturedPiece(last.capturedPiece);
                }
                updateMoveHistory();
            }
        });
        historyPanel.add(undoBtn, BorderLayout.SOUTH);

        historyPanel.setPreferredSize(new Dimension(220, 800));
        add(historyPanel, BorderLayout.EAST);
    }

    // This function updates the move history in real time on the GUI design
    private void updateMoveHistory() {
        moveHistoryArea.setText("");
        for (Move m : moveHistory) moveHistoryArea.append(m + "\n");
    }

    // This function adds a captured piece to a mini graveyard in a side bar in the GUI
    private void addCapturedPiece(Piece piece) {
        JLabel lbl = new JLabel(piece.toString(), SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, getPieceFontSize()-20));
        lbl.setForeground(piece.color == PieceColor.WHITE ? whitePieceColor : blackPieceColor);
        if (piece.color == PieceColor.WHITE) whiteCapturedPanel.add(lbl);
        else blackCapturedPanel.add(lbl);
        whiteCapturedPanel.revalidate();
        blackCapturedPanel.revalidate();
        whiteCapturedPanel.repaint();
        blackCapturedPanel.repaint();
    }

    // This function removes a piece from the chess board (Same color can remove same color because chess rules aren't applied)
    private void removeCapturedPiece(Piece piece) {
        JPanel panel = piece.color == PieceColor.WHITE ? whiteCapturedPanel : blackCapturedPanel;
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JLabel lbl && lbl.getText().equals(piece.toString())) {
                panel.remove(lbl);
                break;
            }
        }
        panel.revalidate();
        panel.repaint();
    }

    // This function gets the size of each piece on the chess board
    private int getPieceFontSize() {
        return switch(boardSize) {
            case SMALL -> 40;
            case MEDIUM -> 60;
            case LARGE -> 80;
        };
    }

    // This function updates the size of each piece on the chess board
    private void updateBoardSize() {
        int dimension = switch(boardSize) {
            case SMALL -> 600;
            case MEDIUM -> 800;
            case LARGE -> 1000;
        };
        boardPanel.setPreferredSize(new Dimension(dimension, dimension));
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++)
                squares[row][col].setFont(new Font("SansSerif", Font.PLAIN, getPieceFontSize()));
        revalidate();
        repaint();
    }

    // This function resets board colors when the user selects "New Game"
    private void resetBoardColors() {
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++)
                squares[row][col].setBackground((row + col) % 2 == 0 ? lightColor : darkColor);
    }

    // This function resets piece colors when the user selects "New Game"
    private void refreshPieceColors() {
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++)
                if(boardMap.containsKey(new Point(row, col)))
                    setPiece(row, col, boardMap.get(new Point(row,col)));
        whiteCapturedPanel.repaint();
        blackCapturedPanel.repaint();
    }

    // This class is the menu at the top of the GUI associated with the SETTINGS
    static class SettingsWindow extends JDialog {
        ChessGameGUI mainGUI;

        public SettingsWindow(ChessGameGUI parent) {
            super(parent, "Settings", true);
            this.mainGUI = parent;
            setSize(400, 300);
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5,5,5,5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            int row = 0;

            // This function allows the user to customize the light tile color
            gbc.gridx=0; gbc.gridy=row; gbc.anchor=GridBagConstraints.EAST;
            add(new JLabel("Light Tile Color:"), gbc);
            JButton lightBtn = new JButton("Choose");
            lightBtn.addActionListener(e -> {
                Color c = JColorChooser.showDialog(this,"Choose Light Tile Color", mainGUI.lightColor);
                if(c!=null) mainGUI.lightColor = c;
                mainGUI.resetBoardColors();
            });
            gbc.gridx=1; gbc.anchor=GridBagConstraints.WEST;
            add(lightBtn, gbc); row++;

            // This function allows the user to customize the dark tile color
            gbc.gridx=0; gbc.gridy=row; gbc.anchor=GridBagConstraints.EAST;
            add(new JLabel("Dark Tile Color:"), gbc);
            JButton darkBtn = new JButton("Choose");
            darkBtn.addActionListener(e -> {
                Color c = JColorChooser.showDialog(this,"Choose Dark Tile Color", mainGUI.darkColor);
                if(c!=null) mainGUI.darkColor = c;
                mainGUI.resetBoardColors();
            });
            gbc.gridx=1; gbc.anchor=GridBagConstraints.WEST;
            add(darkBtn, gbc); row++;

            // This function allows the user to customize the white piece color
            gbc.gridx=0; gbc.gridy=row; gbc.anchor=GridBagConstraints.EAST;
            add(new JLabel("White Piece Color:"), gbc);
            JButton whitePieceBtn = new JButton("Choose");
            whitePieceBtn.addActionListener(e -> {
                Color c = JColorChooser.showDialog(this,"Choose White Piece Color", mainGUI.whitePieceColor);
                if(c!=null) mainGUI.whitePieceColor = c;
                mainGUI.refreshPieceColors();
            });
            gbc.gridx=1; gbc.anchor=GridBagConstraints.WEST;
            add(whitePieceBtn, gbc); row++;

            // This function allows the user to customize the black piece color
            gbc.gridx=0; gbc.gridy=row; gbc.anchor=GridBagConstraints.EAST;
            add(new JLabel("Black Piece Color:"), gbc);
            JButton blackPieceBtn = new JButton("Choose");
            blackPieceBtn.addActionListener(e -> {
                Color c = JColorChooser.showDialog(this,"Choose Black Piece Color", mainGUI.blackPieceColor);
                if(c!=null) mainGUI.blackPieceColor = c;
                mainGUI.refreshPieceColors();
            });
            gbc.gridx=1; gbc.anchor=GridBagConstraints.WEST;
            add(blackPieceBtn, gbc); row++;

            // This function determines the size of the pieces (SMALL, MEDIUM, LARGE)
            gbc.gridx=0; gbc.gridy=row; gbc.anchor=GridBagConstraints.EAST;
            add(new JLabel("Board Size:"), gbc);
            String[] sizes = {"Small","Medium","Large"};
            JComboBox<String> sizeBox = new JComboBox<>(sizes);
            sizeBox.setSelectedItem(mainGUI.boardSize.name().substring(0,1) + mainGUI.boardSize.name().substring(1).toLowerCase());
            sizeBox.addActionListener(e -> {
                String selected = (String) sizeBox.getSelectedItem();
                mainGUI.boardSize = switch(selected) {
                    case "Small" -> BoardSize.SMALL;
                    case "Medium" -> BoardSize.MEDIUM;
                    case "Large" -> BoardSize.LARGE;
                    default -> BoardSize.MEDIUM;
                };
                mainGUI.updateBoardSize();
            });
            gbc.gridx=1; gbc.anchor=GridBagConstraints.WEST;
            add(sizeBox, gbc); row++;

            // Apply button that shows updates in real-time
            JButton applyBtn = new JButton("Apply");
            applyBtn.addActionListener(e -> {
                mainGUI.resetBoardColors();
                mainGUI.updateBoardSize();
                mainGUI.refreshPieceColors();
            });
            gbc.gridx=0; gbc.gridy=row; gbc.gridwidth=2; gbc.anchor=GridBagConstraints.CENTER;
            add(applyBtn, gbc);

            setVisible(true);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChessGameGUI::new);
    }
}
