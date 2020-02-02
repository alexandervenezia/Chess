/**
 * Author: Alexander Venezia
 * 
 * Basic chess game with a computer opponent
 * The opponent's AI is based on the minmax algorithm.
 * 
 * 
 * Possible future additions:
 *  -   Display principal variation
 *  -   Ability to modify time control in application <= Implemented in a basic form>
 *  -   Ability to pit AI versus AI in application
 *  -   Opening book
 *  -   Draw detection for insufficient material, 50 move rule
 *  -   Play back game after completion
 *  -   Ability to play as either color <= Implemented>
 *  -   Fix occasional null pointer error thrown when user selects time control. It seems to be timing related. <= Probably fixed>
 */

package chess;

import javax.swing.JFrame;
import static javax.swing.JFrame.EXIT_ON_CLOSE;

public class Chess extends JFrame {
    private static final int FPS = 60; //Target frames per second
    
    private final Display display;
    private final Board board;
    private final Menu menu;
    
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
        menu = new Menu(display);
        display.setBoard(board);
        display.setMenu(menu);
        
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
        } catch (InterruptedException e) {}
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
