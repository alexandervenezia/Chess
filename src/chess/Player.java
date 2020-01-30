/**
 * Author: Alexander Venezia
 * 
 * Basic chess game with a computer opponent
 * The opponent's AI is based on the minmax algorithm.
 */

package chess;


public interface Player {
    public Move getMoveDecision();
    public Move getPremove();
    public void startThinking();
    public void stopThinking();
    public void clearPremoves();
    public boolean isHuman();
    public void setClock(Timer timer);
}
