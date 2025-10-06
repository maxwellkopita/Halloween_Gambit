package src.pieces;
import src.core.Position;

public abstract class Piece {
    protected final boolean white;
    protected Position position;

    public Piece(boolean white, Position pos){
        this.white = white;
        this.position = pos;
    }

    public boolean isWhite() { return white;}
    public Position gePosition() {return position;}
    
    public abstract boolean canMove(Position from, Position to);
}
