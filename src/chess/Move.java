/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess;

import java.awt.Point;
import java.util.Objects;

/**
 *
 * @author Alexander
 */
public class Move implements Comparable<Move> {
    private final Point startSquare;
    private final Point endSquare;
    
    private double value;
    
    private boolean isFirstKingMove;
    private boolean isFirstRookMove;
    private boolean enPassant;
    private boolean promotion;
    private char enPassantVal; //0-7 for enabling en passant on a specific file, ' ' for disabling it.
    private int castleVal;
    private char capturedPiece;
    private char movingPiece;
    private char promotingTo;
    
    private boolean isAbortion;
    
    private static final char[] COORD_NOTATION = new char[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'};
    
    //Game is over
    public Move()
    {
        startSquare = null;
        endSquare = null;
        value = -1;
        movingPiece = 'x';
        isAbortion = false;
        capturedPiece = ' ';
    }
    
    public Move(Point start, Point end)
    {
        startSquare = start;
        endSquare = end;
        movingPiece = 'v';
        isAbortion = false;
        capturedPiece = ' ';
        
    }
    
    
    public Point getStartSquare()
    {
        return startSquare;
    }
    
    public Point getEndSquare()
    {
        return endSquare;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        
        Move move = (Move) o;
        
        if (move.startSquare == null || move.getEndSquare() == null)
            return false;
        
        return (startSquare.x == move.startSquare.x && 
                startSquare.y == move.startSquare.y &&
                endSquare.x == move.endSquare.x &&
                endSquare.y == move.endSquare.y);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.startSquare);
        hash = 17 * hash + Objects.hashCode(this.endSquare);
        return hash;
    }
    
    private static String toNotation(Point coords)
    {
        //System.out.println(coords.x);
        return COORD_NOTATION[coords.x] + "" + (8-coords.y);// + Character.forDigit(coords.y, 10);
    }
    
    @Override
    public String toString()
    {
        //return startSquare + " " + endSquare;
        return toNotation(startSquare) + "-" + toNotation(endSquare);
    }
    
    public void setValue(double value)
    {
        this.value = value;
    }
    
    public double getValue()
    {
        return value;
    }
    
    public char getCapturedPiece()
    {
        return capturedPiece;
    }
    
    public void setCapturedPiece(char piece)
    {
        capturedPiece = piece;
    }

    /**
     * @return the isFirstKingMove
     */
    public boolean isFirstKingMove() {
        return isFirstKingMove;
    }

    /**
     * @param isFirstKingMove the isFirstKingMove to set
     */
    public void setFirstKingMove(boolean isFirstKingMove) {
        this.isFirstKingMove = isFirstKingMove;
    }

    /**
     * @return the isFirstRookMove
     */
    public boolean isFirstRookMove() {
        return isFirstRookMove;
    }

    /**
     * @param isFirstRookMove the isFirstRookMove to set
     */
    public void setFirstRookMove(boolean isFirstRookMove) {
        this.isFirstRookMove = isFirstRookMove;
    }

    /**
     * @return the castleVal
     */
    public int getCastleVal() {
        return castleVal;
    }

    /**
     * @param castleVal the castleVal to set
     */
    public void setCastleVal(int castleVal) {
        this.castleVal = castleVal;
    }

    /**
     * @return the enPassant
     */
    public boolean isEnPassant() {
        return enPassant;
    }

    /**
     * @param enPassant the enPassant to set
     */
    public void setEnPassant(boolean enPassant) {
        this.enPassant = enPassant;
    }

    /**
     * @return the promotion
     */
    public boolean isPromotion() {
        return promotion;
    }

    /**
     * @param promotion the promotion to set
     */
    public void setPromotion(boolean promotion) {
        this.promotion = promotion;
    }

    /**
     * @return the enPassantVal
     */
    public char getEnPassantVal() {
        return enPassantVal;
    }

    /**
     * @param enPassantVal the enPassantVal to set
     */
    public void setEnPassantVal(char enPassantVal) {
        this.enPassantVal = enPassantVal;
    }
    
    private int getPieceValue(char piece)
    {
        switch (Character.toLowerCase(piece))
        {
            case 'p':
                return 100;
            case 'n':
                return 300;
            case 'b':
                return 350;
            case 'r':
                return 500;
            case 'q':
                return 900;
            case 'k':
                return 1000000;
        }
        return 0;
    }

    @Override
    public int compareTo(Move o) {
        int x;
       
        
        if (getPieceValue(o.getMovingPiece()) == 0)
            o.setMovingPiece('p');
        if (getPieceValue(movingPiece) == 0)
            movingPiece = 'p';
        
        x = (int)(getPieceValue(capturedPiece)/getPieceValue(movingPiece) - getPieceValue(o.getCapturedPiece())/getPieceValue(o.getMovingPiece()));
        
        return -x;
    }

    /**
     * @return the movingPiece
     */
    public char getMovingPiece() {
        return movingPiece;
    }

    /**
     * @param movingPiece the movingPiece to set
     */
    public void setMovingPiece(char movingPiece) {
        this.movingPiece = movingPiece;
    }
    
    public void setAbortion()
    {
        isAbortion = true;
    }
    
    public boolean isAbortion()
    {
        return isAbortion;
    }
    
    public Move copy()
    {
        Move m;
        
        if (startSquare != null)
        {
            m = new Move(startSquare, endSquare);
        }
        else
        {
            m = new Move();
        }
        m.setFirstKingMove(isFirstKingMove);
        m.setCapturedPiece(capturedPiece);
        m.setCastleVal(castleVal);
        m.setEnPassant(enPassant);
        m.setEnPassantVal(enPassantVal);
        m.setFirstRookMove(isFirstRookMove);
        m.setMovingPiece(movingPiece);
        m.setPromotion(promotion);
        m.setValue(value);
        m.setPromotingTo(promotingTo);
        if (isAbortion)
            m.setAbortion();

        return m;
    }
    
    public String getAlgebraic()
    {
        String notation = "";
        
        if (movingPiece != 'p' && movingPiece != 'P')
            notation += Character.toUpperCase(movingPiece);
        
        notation += COORD_NOTATION[startSquare.x];
        notation += (8-startSquare.y);
        
        if (capturedPiece != ' ')
        {
            //if (movingPiece == 'p' || movingPiece == 'P')
            //    notation += COORD_NOTATION[startSquare.x];
            notation += "x";
        }
        else
            notation += "-";
        
        notation += COORD_NOTATION[endSquare.x];
        notation += (8-endSquare.y);
        
        return notation;
    }
    
    public char getPromotingTo()
    {
        System.out.println(promotingTo);
        return promotingTo;
    }
    
    public void setPromotingTo(char promotingTo)
    {
        this.promotingTo = promotingTo;
    }
}
