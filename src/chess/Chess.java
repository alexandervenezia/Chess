/**
 * Basic chess game with a computer opponent
 * The opponent's AI is based on the minmax algorithm.
 */
package chess;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import static javax.swing.JFrame.EXIT_ON_CLOSE;

public class Chess extends JFrame {
    private static final int FPS = 60; //Target frames per second
    
    private final Display display;
    private final Board board;
    
    private long lastTime; //Time of the last frame
    
    private static int gameWidth;
    private static int gameHeight;
    
    
    public Chess()
    {
        super("Chess");
        lastTime = System.nanoTime();
 
        gameWidth = 1200; //1200
        gameHeight = 950; //950
        display = new Display(gameWidth, gameHeight);
        board = new Board(display);
        display.setBoard(board);
        
        super.add(display);
    }
    
    private void update()
    {
        /*FPS control*/
        long time = System.nanoTime(); //Current time
        long updateTime = time-lastTime; //Elapsed time since last frame
        double delta = updateTime*0.000001; //Convert to milliseconds
        lastTime = time;
       
        display.update(delta);
        
        //If frame is executed too quickly, wait so that FPS is maintained
        long waitTime = (long)(1000/FPS-(updateTime*0.000001));
        
        if (waitTime > 0)
            try {
                Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            Logger.getLogger(Chess.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    public static void main(String[] args) {
        Chess game = new Chess();
        
        game.setDefaultCloseOperation(EXIT_ON_CLOSE);

        game.pack();
        game.setVisible(true);
        game.setResizable(true);
        
        //Mainloop
        while (true)
        {
            game.update();
        }
    }
    
    public static int getGameWidth()
    {
        return gameWidth;
    }
    
    public static int getGameHeight()
    {
        return gameHeight;
    }
    
        
    
}
