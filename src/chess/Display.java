/**
 * Author: Alexander Venezia
 * 
 * Basic chess game with a computer opponent
 * The opponent's AI is based on the minmax algorithm.
 */

package chess;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JPanel;

public class Display extends JPanel {
    //Fonts used for UI elements
    private final static java.awt.Font FONT = new java.awt.Font("Cambria", java.awt.Font.BOLD, 50);    
    private final static java.awt.Font SMALL_FONT = new java.awt.Font("Cambria", java.awt.Font.BOLD, 35);
    private final static java.awt.Font TINY_FONT = new java.awt.Font("Cambria", java.awt.Font.BOLD, 20);
    
    //Colors used for UI elements
    private static final Color TRANSPARENT_COL = new Color(255, 255, 255, 100);
    private static final Color BACKGROUND_COL = new Color(5, 40, 3);
    private static final Color BROWN_COL = new Color(60, 35, 15);
    
    //Dimensions of the display
    private final int DISPLAY_WIDTH; 
    private final int DISPLAY_HEIGHT; 
    
    private Board board;
    private Menu menu;
    
    private boolean started;
                            
    public Display(int width, int height)
    {       
        this.DISPLAY_WIDTH = width;
        this.DISPLAY_HEIGHT = height;
        started = false;
        super.setBackground(BACKGROUND_COL);
    }
    
    public void update(double delta)
    {
        if (started)
            board.update(delta);
        else
            menu.update();
        paintImmediately(0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT); //Redraw graphics   
    }
    private void render(Graphics g)
    {
        super.paintComponent(g);
        if (started)
            board.render(g);
        else
            menu.render(g);
    }
    
    private void renderUI(Graphics g) {}
        
    @Override
    public void paintComponent(Graphics g)
    {      
        render(g);
        renderUI(g);
    }
    
    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(DISPLAY_WIDTH, DISPLAY_HEIGHT);
    }
    
    public void setBoard(Board board)
    {
        this.board = board;
    }
    
    public void setMenu(Menu menu)
    {
        this.menu = menu;
    }
    
    public void start(int whiteMinutes, int blackMinutes, int whiteSeconds, int blackSeconds, int increment, int delay, boolean playerIsWhite)
    {
        if (!started)
        {
            board.setTimeControl(whiteMinutes, blackMinutes, whiteSeconds, blackSeconds, increment, delay);
            board.setColors(playerIsWhite);
            started = true;
            board.start(this);
            this.removeMouseListener(menu);
            
            menu = null;
        }        
    }
}
