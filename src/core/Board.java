package src.core;

import src.pieces.Piece;

public class Board {
    private final String[][] grid = new String[8][8];

    public Board() {
        setupStart();
    }

    private void setupStart(){
        // Empty
        for (int r = 0; r < 8; r++){
            for (int f = 0; f<8; f++){
                if ((r+f) % 2 == 0){
                    grid[r][f] = "  ";
                }else {
                    grid[r][f] = "##";
                }
                
            }
        }
        // Pawns
        for(int f=0; f<8; f++){
            grid[1][f] = "bP";
            grid[6][f] = "wP";
        }
        // Rooks
        grid[0][0] = "bR"; grid[0][7] = "bR"; grid[7][0] = "wR"; grid[7][7] = "wR";
        // Knights
        grid[0][1] = "bN"; grid[0][6] = "bN"; grid[7][1] = "wN"; grid[7][6] = "wN";
        // Bishops
        grid[0][2] = "bB"; grid[0][5] = "bB"; grid[7][2] = "wB"; grid[7][5] = "wB";
        // Queens
        grid[0][3] = "bQ"; grid[7][3] = "wQ";
        // Kings
        grid[0][4] = "bK"; grid[7][4] = "wK";
    }

    public void display(){
        System.out.println("  A  B  C  D  E  F  G  H");
        for (int r = 0; r <= 7; r++){
            System.out.print((8-r) + " ");
            for(int f = 0; f < 8; f++){
                System.out.print(grid[r][f] + " " );
            }
            System.out.println();
        }
    }

    public boolean movePiece(Position from, Position to){
        return true;
    }
}
