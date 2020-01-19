/**
 * Author: Alexander Venezia
 * 
 * Basic chess game with a computer opponent
 * The opponent's AI is based on the minmax algorithm.
 */

package chess;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class Menu implements MouseListener {
    private class Button
    {
        private final java.awt.Font font = new java.awt.Font("Cambria", java.awt.Font.BOLD, 50);   
        private final Color outerColor = new Color(50, 75, 200);
        //private final Color innerColor
        
        private final int[] position;
        private final int width;
        private final int height;
        private String message;
        
        private Button(int[] position, int width, int height)
        {
            this.position = position;
            this.width = width;
            this.height = height;
            message = "";
        }        
        
        protected void render(Graphics g)
        {
            g.setColor(outerColor);
            g.fillRoundRect(position[0], position[1], width, height, 20, 20);
            
            g.setColor(Color.WHITE);
            g.setFont(font);
            
            FontMetrics metrics = g.getFontMetrics();
            
            int x = position[0] + (width - metrics.stringWidth(message)) / 2;
            int y = position[1] + ((height - metrics.getHeight()) / 2) + (int)(metrics.getAscent()*0.7);
            
            g.drawString(message, x, y);
        }
        
        protected void onClick()
        {
            
        }
        
        private void isClicked(int x, int y)
        {
            if (x > position[0] && y > position[1] && x < position[0]+width && y < position[1]+height)
                onClick();
        }
    }
    
    private class TimeControlButton extends Button
    {
        private static final int WIDTH = 300;
        private static final int HEIGHT = 250;
        
        private final int minutes;
        private final int seconds;
        private final int delay;
        private final int increment;
        
        
        private TimeControlButton(int[] position, int minutes, int seconds, int delay, int increment)
        {
            super(position, WIDTH, HEIGHT);            
            super.message = determineMessage(minutes, seconds, delay, increment);
            
            this.minutes = minutes;
            this.seconds = seconds;
            this.delay = delay;
            this.increment = increment;
        }
        
        private String determineMessage(int minutes, int seconds, int delay, int increment)
        {
            String msg = "";
            
            if (minutes > 0)
                msg += minutes;
            
            if (seconds > 0)
            {
                if (minutes > 0)
                    msg += ":";

                if (seconds < 10)
                    msg += "0";
                msg += seconds;
            }
            
            if (increment > 0)
            {
                msg += " + " + increment;
            }
            
            if (delay > 0)
            {
                msg += " + " + delay + "d";
            }
            
            return msg;
        }
        
        @Override
        protected void onClick()
        {
            display.start(minutes, minutes, seconds, seconds, increment, delay);
        }
    }
    
    private final TimeControlButton bulletTime = new TimeControlButton(new int[]{150, 100}, 1, 0, 0, 0);
    private final TimeControlButton bulletTimeDelay = new TimeControlButton(new int[]{460, 100}, 1, 0, 2, 0);
    
    private final TimeControlButton threeMinute = new TimeControlButton(new int[]{770, 100}, 3, 0, 0, 0);
    private final TimeControlButton threeMinuteIncrement = new TimeControlButton(new int[]{150, 360}, 3, 0, 0, 2);
    
    private final TimeControlButton fiveMinute = new TimeControlButton(new int[]{460, 360}, 5, 0, 0, 0);
    private final TimeControlButton fiveMinuteIncrement = new TimeControlButton(new int[]{770, 360}, 5, 0, 0, 3);
    
    private final TimeControlButton tenMinuteDelay = new TimeControlButton(new int[]{150, 620}, 10, 0, 5, 0);
    private final TimeControlButton fifteenMinuteIncrement = new TimeControlButton(new int[]{460, 620}, 15, 0, 0, 5);
    
    private final Button customTime = new Button(new int[]{770, 620}, 300, 250);
    
    private final Button[] buttons = new Button[] {bulletTime, bulletTimeDelay, threeMinute, threeMinuteIncrement, fiveMinute, fiveMinuteIncrement, tenMinuteDelay, fifteenMinuteIncrement, customTime};
    
    protected final Display display;
    
    public Menu(Display display)
    {
        customTime.message = "Custom";
        
        this.display = display;
        display.addMouseListener(this);
    }
    
    public void render(Graphics g)
    {
        for (Button b : buttons)
            b.render(g);
    }
    
    public void update() {}

    @Override
    public void mouseClicked(MouseEvent me) {}

    @Override
    public void mousePressed(MouseEvent me) {}

    @Override
    public void mouseReleased(MouseEvent me)
    {
        for (Button b : buttons)
            b.isClicked(me.getX(), me.getY());
    }

    @Override
    public void mouseEntered(MouseEvent me) {}

    @Override
    public void mouseExited(MouseEvent me) {}
    
}
