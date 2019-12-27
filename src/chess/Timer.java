/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alexander
 */
public class Timer implements Runnable {
    private double whiteTime;
    private double blackTime;
    private int increment;
    private int delay;
    public double currentDelay;
    
    private long startTime;
    
    private boolean running = false;
    
    private Thread thread;
    
    private boolean started = false;
    private boolean whiteClockRunning = true;
    
    private int flips = 0;
    
    public Timer(double whiteTime, double blackTime, int increment, int delay)
    {
        this.whiteTime = whiteTime;
        this.blackTime = blackTime;
        this.increment = increment;
        this.delay = delay;
        currentDelay = delay*1000;
        
        thread = new Thread(this);
        
        if (whiteTime > 300*1000)
        {
            start();
            started = true;
        }
    }
    @Override
    public void run() {
        while (running)
        {
            if (started)
            {
                if (currentDelay <= 0)
                {
                    if (whiteClockRunning)
                    {
                        whiteTime -= (System.nanoTime()-startTime)/1000000;
                    }
                    else
                    {
                        blackTime -= (System.nanoTime()-startTime)/1000000;
                    }
                }
                else
                {
                    currentDelay -= (System.nanoTime()-startTime)/1000000;
                    
                }
            }
            startTime = System.nanoTime();
            try {
                Thread.sleep(5);
            } catch (InterruptedException ex) {
                Logger.getLogger(Timer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void start()
    {
        if (!running)
        {
            thread.start();
            running = true;
            startTime = System.nanoTime();
        }
    }
    
    public void stop()
    {
        running = false;
    }
    
    public boolean isRunning()
    {
        return running;
    }
    
    public int getIncrement()
    {
        return increment;
    }
    
    public int getDelay()
    {
        return delay;
    }
    
    public void flip()
    {
        if (whiteClockRunning)
            whiteTime += increment*1000;
        else
            blackTime += increment*1000;
        
        currentDelay = delay*1000;
        
        whiteClockRunning = !whiteClockRunning;
        startTime = System.nanoTime();
        
        flips++;
        
        if (flips > 1)
        {
            started = true;
        }
    }
    
    public double getWhiteTime()
    {
        return whiteTime/1000;
    }
    
    public double getBlackTime()
    {
        return blackTime/1000;
    }
    
}
