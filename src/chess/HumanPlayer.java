/**
 * Author: Alexander Venezia
 * 
 * Basic chess game with a computer opponent
 * The opponent's AI is based on the minmax algorithm.
 */

package chess;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


class HumanPlayer implements Player, MouseListener{
    private Move move;
    private Move premove;
    private boolean heldDown;
    private boolean isThinking;
    private boolean isWhite;
    
    public HumanPlayer(boolean isWhite)
    {
        this.isWhite = isWhite;
        isThinking = false;
        heldDown = false;
    }
    
    @Override
    public Move getMoveDecision()
    {
        return move;
    }
    
    @Override
    public void startThinking()
    {
        isThinking = true;
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        heldDown = true;
        if (isThinking)
        {
            if (e.getButton() == 1)
            {
                if (Board.hasSelected() && move == null)
                {
                    Move possibleMove = new Move((Point) Board.getSelected().clone(), Board.toBoardCoords(e.getPoint()));
                    if (Board.isLegalMove(possibleMove, isWhite))
                    {
                        move = possibleMove;
                        Board.deSelect();
                    }
                    else
                        Board.setSelected(e.getPoint());
                }
                else
                    Board.setSelected(e.getPoint());
            }
            else
                Board.deSelect();
        }
        else
        {
            if (e.getButton() == 1)
            {
                if (Board.hasSelected() && premove == null)
                {
                    premove = new Move((Point) Board.getSelected().clone(), Board.toBoardCoords(e.getPoint()));

                    Board.deSelect();
                }
                else
                    Board.setSelected(e.getPoint());
            }
            else
            {
                Board.deSelect();
                premove = null;
            }
            
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        heldDown = false;
        
        Board.checkPromotionSelection(e.getPoint());
        
        if (isThinking)
        {
            if (e.getButton() == 1)
            {
                if (Board.hasSelected() && move == null)
                {
                    Move possibleMove = new Move((Point) Board.getSelected().clone(), Board.toBoardCoords(e.getPoint()));
                    if (Board.isLegalMove(possibleMove, isWhite))
                    {
                        move = possibleMove;
                        Board.deSelect();
                    }
                }
            }
        }
        else
        {
            if (e.getButton() == 1)
            {
                if (Board.hasSelected())
                {
                    if (premove != null)
                        premove = null;
                    else
                        premove = new Move((Point) Board.getSelected().clone(), Board.toBoardCoords(e.getPoint()));
                    Board.deSelect();
                }
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
    
    public boolean isMouseHeldDown()
    {
        return heldDown;
    }

    @Override
    public void stopThinking() {
        isThinking = false;
        move = null;
        premove = null;
    }
    
    public void clearPremoves()
    {
        premove = null;
    }
    
    @Override
    public Move getPremove()
    {
        return premove;
    }
    
}
