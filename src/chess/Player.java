/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess;

/**
 *
 * @author Alexander
 */
public interface Player {
    public Move getMoveDecision();
    public Move getPremove();
    public void startThinking();
    public void stopThinking();
}
