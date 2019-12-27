/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alexander
 */
public class ComputerPlayer implements Player, Runnable {
    private boolean isBook;
    private double isOpening;
    private double isMiddlegame;
    private double isEndgame;    
    
    private boolean isThinking;
    private Move move;
    private static int movesAnalyzed;
    private boolean isWhite;
    private final Thread thread;
    
    private static final int PROCESSING_THREADS = 0;
    private int thinkTime = 4000;
    private int maxThinkTime = 10000;//THINK_TIME*3;
    private int minThinkTime;
    private static final int EXPECTED_TIME_MULT = 30;
    private static final int EXPECTED_TIME_MULT_ENDGAME = 25;
    
    private Thread[] processingThreads;
    private LinkedList<Move>[] processingThreadMoves;
    private Move[] processingThreadChoice;
    private boolean[] processingThreadsComplete;
    
    //private static Move[] principalVariation;
    private static LinkedList<Move> principalVariation;
    
    private static int totalMovesAnalyzed;
    private static double totalTime;
    
    private int moves;
    
    private static int leaves;
    
    private boolean abortSearch;
    
    private int threadCount = 0;
    
    private long searchStartTime;
    
    public static Move lastAnalyzed;
    
    private static final int[] WINDOW_SIZES = new int[]{20, 20, 20, 15, 15, 15, 15};
    
    private Timer clock;
    
    private static final double[][] CENTRALIZATION = new double[][]
    {
        {0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00},
        {0.00, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.00},
        {0.00, 0.25, 0.75, 0.75, 0.75, 0.75, 0.25, 0.00},
        {0.00, 0.25, 0.75, 1.00, 1.00, 0.75, 0.25, 0.00},
        {0.00, 0.25, 0.75, 1.00, 1.00, 0.75, 0.25, 0.00},
        {0.00, 0.25, 0.75, 0.75, 0.75, 0.75, 0.25, 0.00},
        {0.00, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.00},
        {0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00},
        
    };
    
    private static final double[][] BISHOP_POSITION_VALUES = new double[][]
    {
        {0.10, 0.10, 0.00, 0.10, 0.10, 0.00, 0.10, 0.10},
        {0.20, 1.20, 0.10, 0.30, 0.30, 0.10, 1.20, 0.20},
        {0.80, 0.80, 0.10, 0.35, 0.35, 0.10, 0.80, 0.80},
        {0.15, 0.30, 0.75, 0.20, 0.20, 0.75, 0.30, 0.15},
        {0.15, 0.30, 0.75, 0.20, 0.20, 0.75, 0.30, 0.15},
        {0.80, 0.80, 0.10, 0.35, 0.35, 0.10, 0.80, 0.80},
        {0.20, 1.20, 0.10, 0.30, 0.30, 0.10, 1.20, 0.20},
        {0.10, 0.10, 0.00, 0.10, 0.10, 0.00, 0.10, 0.10},
    };
    
    public ComputerPlayer(boolean isWhite)
    {
        this.isWhite = isWhite;
        thread = new Thread(this);
        leaves = 0;
        movesAnalyzed = 0;
        
        moves = 0;
        
        isBook = false;
        isOpening = 100;
        isMiddlegame = 0;
        isEndgame = 0;
        
        processingThreads = new Thread[PROCESSING_THREADS];
        processingThreadMoves = new LinkedList[PROCESSING_THREADS];
        processingThreadsComplete = new boolean[PROCESSING_THREADS];
        processingThreadChoice = new Move[PROCESSING_THREADS];
        
        for (int i = 0; i < processingThreads.length; i++)
        {
            processingThreads[i] = new Thread(this);
            processingThreadMoves[i] = new LinkedList<Move>();
            processingThreadsComplete[i] = false;
            processingThreadChoice[i] = null;
        }
        
        principalVariation = new LinkedList<Move>();
        abortSearch = false;
                
    }
    
    private double lerp(double value1, double value2, double factor)
    {
        return (1-factor) * value1 + factor * value2;
    }
    
    @Override
    public void run()
    {
        int thisThread = threadCount;
        threadCount++;
        
        if (threadCount == 1)
        {
            //System.out.println("T");
            while (true)
            {
                if (isThinking && move == null)
                {
                    char[][] position = Board.getBoardPosition();
                    long zobrist = Board.getZobrist();

                    LinkedList<Move> possibleMoves = Board.getLegalMoves(position, isWhite, true);

                    move = determineMove(position, possibleMoves, zobrist);
                }
                else
                {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ComputerPlayer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        else
        {
            while (true)
            {
                if (!processingThreadsComplete[thisThread-1] && processingThreadChoice[thisThread-1] == null && processingThreadMoves[thisThread-1].size() > 0)
                {
                    char[][] position = Board.getBoardPosition();
                    long zobrist = Board.getZobrist();
                    
                    long startTime = System.nanoTime();
                    double timeTaken = 0;
                    
                    int currentDepth = 0;
        
                    int alpha = -10000000;
                    int beta = 10000000;
                    
                    isOpening = isOpening(position);
                    isEndgame = isEndgame(position);
                    
                    Move choice = null;


                    while ((timeTaken*lerp(EXPECTED_TIME_MULT, EXPECTED_TIME_MULT_ENDGAME, isEndgame*0.01) < thinkTime || timeTaken < minThinkTime) && !abortSearch)
                    {
                        currentDepth++;

                        choice = minmax(position, 0, currentDepth, isWhite, alpha, beta, processingThreadMoves[thisThread-1], true, zobrist);
                        
                        if (!choice.isAbortion())
                            processingThreadChoice[thisThread-1] = choice;
                        
                        reorderMoves(processingThreadMoves[thisThread-1], processingThreadChoice[thisThread-1]);
                        timeTaken = (System.nanoTime()-startTime)/1000000;
                    }
                    processingThreadsComplete[thisThread-1] = true;
                    //System.out.println(thisThread-1 + " " + currentDepth);
                }
            }
        }
    }
    
    private void reorderMoves(LinkedList<Move> legalMoves, Move bestMove)
    {
        legalMoves.remove(bestMove);
        legalMoves.add(0, bestMove);
    }
    
    private void determineThinkTime()
    {
        double timeLeft;
        double maxMult = 2;
        
        if (isWhite)
        {
            System.out.println("White");
            timeLeft = clock.getWhiteTime()*1000;
        }
        else
        {
            System.out.println("Black");
            timeLeft = clock.getBlackTime()*1000;
        }
        
        int delay = clock.getDelay()*1000;
        int increment = clock.getIncrement()*1000;                
        
        thinkTime = (int)timeLeft/Math.max((30-moves), 5);
        
        if (timeLeft < 60*1000)
            maxMult = 1.5; 
        else if (timeLeft < 180*1000)
            maxMult = 2;
        else if (timeLeft < 600*1000)
            maxMult = 2.5;
        else
            maxMult = 3;
        
        maxThinkTime = (int)(thinkTime*maxMult);
        
        if (timeLeft < 10*1000 && increment == 0 && delay == 0)
        {
            thinkTime = 500;
            maxThinkTime = 500;
            
            if (timeLeft < 4*1000)
            {
                thinkTime = 200;
                maxThinkTime = 200;
            }
        }
        
        if (increment > 0)
        {
            if (timeLeft > 180*1000)
            {
                minThinkTime = thinkTime/3;
                maxThinkTime = (int)(thinkTime*maxMult);
            }
            else if (timeLeft > 120*1000)
            {
                minThinkTime = thinkTime/4;
                maxThinkTime = (int)(thinkTime*1.8);
            }
            else if (timeLeft > 90*1000)
            {
                minThinkTime = thinkTime/5;
                maxThinkTime = (int)(thinkTime*1.8);
            }
        }
        
        thinkTime += increment;
        maxThinkTime += increment;
        
        if (thinkTime < delay)
        {
            thinkTime = delay;
            minThinkTime = thinkTime/2;
            if (maxThinkTime < thinkTime)
                maxThinkTime += delay;
            
            if (timeLeft < 10*1000)
            {
                maxThinkTime = delay;
            }
        }
        
        //minThinkTime = delay/2;
        minThinkTime = 0;
        
        if (increment > 0)
        {
            if (timeLeft < 10*1000)
            {
                System.out.println("Low time");
                if (maxThinkTime > increment)
                {
                    thinkTime = increment;
                    maxThinkTime = increment;
                }
            }
        }
            
        System.out.println("Target time: " + thinkTime/1000 + "; Minimum time: " + minThinkTime/1000 + "; Maximum time: " + maxThinkTime/1000 + "\n\n");
    }
    
    private Move determineMove(char[][] position, LinkedList<Move> legalMoves, long zobrist)
    {
        if (legalMoves.size() == 1)
            return legalMoves.get(0);
        
        determineThinkTime();
        
        long startTime = System.nanoTime();
        searchStartTime = startTime;
        int moveCount = 0;
        int remainder = 0;
        LinkedList<Move> mainMoves = new LinkedList<>();
        
        for (int i = 0; i < legalMoves.size()/(processingThreads.length+1); i++)
        {
            mainMoves.add(legalMoves.get(moveCount));
            moveCount++;
        }
        
        for (int i = 0; i < processingThreads.length; i++)
        {
            for (int j = 0; j < legalMoves.size()/(processingThreads.length+1); j++)
            {
                processingThreadMoves[i].add(legalMoves.get(moveCount));
                moveCount++;
            }
        }
        
        remainder = legalMoves.size()-moveCount;
        
        for (int i = 0; i < remainder; i++)
        {
            mainMoves.add(legalMoves.get(moveCount));
            moveCount++;
        }
        
        for (int i = 0; i < processingThreads.length; i++)
        {
            if (processingThreadMoves[i].size() == 0)
            {
                processingThreadsComplete[i] = true;
            }
        }
        
        leaves = 0;
        movesAnalyzed = 0;
        Move choice = null;
        Move newChoice = null;
        
        
        double timeTaken = 0;
        
        int currentDepth = -1;
        
        int alpha = -10000000;
        int beta = 10000000;
        
        isOpening = isOpening(position);
        isEndgame = isEndgame(position);
        
        System.out.println(lerp(EXPECTED_TIME_MULT, EXPECTED_TIME_MULT_ENDGAME, isEndgame*0.01));
        while ((timeTaken*lerp(EXPECTED_TIME_MULT, EXPECTED_TIME_MULT_ENDGAME, isEndgame*0.01) < thinkTime || timeTaken < minThinkTime) && !abortSearch)
        {/*
            if (choice != null)
            {
                if (currentDepth%2 == 0)
                {
                    alpha = (int)(choice.getValue()-WINDOW_SIZES[Math.min(currentDepth-1, WINDOW_SIZES.length-1)]);
                    beta = (int)(choice.getValue()+WINDOW_SIZES[Math.min(currentDepth-1, WINDOW_SIZES.length-1)]);
                    System.out.println(choice.getValue());
                }
            }*/
            
            currentDepth+=2;
                        
            newChoice = minmax(position, 0, currentDepth, isWhite, alpha, beta, mainMoves, true, zobrist);
            
            
            if (!newChoice.isAbortion())
            {         
                choice = newChoice;
                
                if (newChoice.getValue() == 0)
                {
                    System.out.println("ZERO");
                }
            }
            else
            {
                currentDepth-=2;
                if (choice.getValue() == 0)
                {
                    System.out.println("CHANGE ERROR ");
                }
            }
            
            /*
            if (choice.getValue() <= alpha || choice.getValue() >= beta)
            {
                currentDepth--;
                
                System.out.println("Repeating depth " + currentDepth);
                System.out.println(alpha + " " + beta + " " + choice.getValue());
                
                alpha = -10000000;
                beta = 10000000;
                
                continue;
            }*/
            
            //reorderMoves(mainMoves, choice);
            timeTaken = (System.nanoTime()-startTime)/1000000;
                        
            System.out.println("Depth: " + currentDepth + "; Time: " + timeTaken + " " + newChoice);
            
        }
        
        
        System.out.println("Main");
        
        boolean exit = true;
                
        while (true)
        {
            exit = true;
            for (int i = 0; i < processingThreadsComplete.length; i++)
            {
                if (!processingThreadsComplete[i])
                    exit = false;
            }
            
            if (exit)
            {
                break;
            }
        }
                
        for (int i = 0; i < processingThreads.length; i++)
        {
            if (processingThreadChoice[i] != null)
            {
                if ((processingThreadChoice[i].getValue() > choice.getValue())^(!isWhite))
                {
                    if (abortSearch)
                        if (choice.getValue() == 0)
                            System.out.println("ERR ERR ERR ERR ERR ERR ERR " + i + " " + choice.isAbortion());
                    
                    choice = processingThreadChoice[i];
                    
                }
            }
        }
                
        for (int i = 0; i < processingThreads.length; i++)
        {
            processingThreadsComplete[i] = false;
            processingThreadMoves[i].clear();
            processingThreadChoice[i] = null;
        }
        
        abortSearch = false;
        
        timeTaken = (System.nanoTime()-startTime)/1000000;
        
        totalMovesAnalyzed += movesAnalyzed;        
        totalTime += timeTaken;
        
        
        System.out.println("Move chosen: " + choice);
        System.out.println("Depth reached: " + currentDepth);
        System.out.println("Leaves: " + leaves);
        System.out.println("Evaluation: " + choice.getValue());
        System.out.println("Rough eval: " + evaluateLeaf(position));
        System.out.println("Moves analyzed: " + movesAnalyzed);
        System.out.println("Time taken (ms): " + timeTaken);
        System.out.println("Time per move: " + timeTaken/movesAnalyzed);
        System.out.println("Opening percent: " + isOpening(position));
        System.out.println("Middlegame percent: " + isMiddlegame(position));
        System.out.println("Endgame percent: " + isEndgame(position));
        
        System.out.println("Avg time per move: " + totalTime/totalMovesAnalyzed+"\n\n");
        System.out.println("\n" + Board.getMoves()+"\n");
        
        return choice;
    }
    
    private static int totalMaterial(char[][] position)
    {
        int material = 0;
        
        for (int i = 0; i < 8; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                switch (Character.toLowerCase(position[i][j]))
                {
                    case 'p':
                        material++;
                        break;
                    case 'n':
                        material+=3;
                        break;
                    case 'b':
                        material+=3;
                        break;
                    case 'r':
                        material+=5;
                        break;
                    case 'q':
                        material+=9;
                        break;
                }
            }
        }
        
        return material;
    }
    
    private static int materialOnStartingSquares(char[][] position)
    {
        int material = 0;
        
        for (int i = 0; i < 8; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                if (Board.STARTING_POSITION[i][j] == position[i][j])
                {
                    switch (Character.toLowerCase(position[i][j]))
                    {
                        case 'p':
                            material++;
                            break;
                        case 'n':
                            material+=3;
                            break;
                        case 'b':
                            material+=3;
                            break;
                        case 'r':
                            material+=5;
                            break;
                        case 'q':
                            material+=9;
                            break;
                    }
                }
            }
        }
        
        return material;
    }
    
    private static double isOpening(char[][] position)
    {
        double openingValue = 0;
        
        openingValue += totalMaterial(position)-70;
        
        openingValue += materialOnStartingSquares(position);
                
        return (openingValue/86)*100;
    }
    
    private static double isMiddlegame(char[][] position)
    {
        double middlegameValue = 0;
        
        middlegameValue += (totalMaterial(position)-25);
        middlegameValue -= (materialOnStartingSquares(position)-25);
        
        return (middlegameValue/45)*100;
    }
    
    private static double isEndgame(char[][] position)
    {
        double endgameValue = 0;
        
        endgameValue = 78-totalMaterial(position);
        
        return (endgameValue/78)*100;
    }
    
    private double evaluateLeaf(char[][] position)
    {        
        int availableMoves;
        
        leaves++;
        double value = 0;
        
        for (int i = 0; i < 8; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                switch (position[i][j])
                {
                    case 'p':
                        value += 17;
                        value += CENTRALIZATION[i][j]*4;
                        value -= i*1;
                        break;
                    case 'P':
                        value -= 17;
                        value -= CENTRALIZATION[i][j]*4;
                        value += 7-(i*1);
                        
                        break;
                    case 'n':
                        value += 35;
                        value += (CENTRALIZATION[i][j]-0.5)*5;
                        
                        if (i == 4)
                            value += 5;
                        if (i == 5 || i == 6)
                            value += 10;
                        
                        availableMoves = Board.findKnightMoves(position, true, j, i);
                        
                        value += (availableMoves-2)*2;
                        
                        break;
                    case 'N':
                        value -= 35;
                        
                        value -= (CENTRALIZATION[i][j]-0.5)*5;
                        
                        if (i == 3)
                            value -= 5;
                        if (i == 2 || i == 1)
                            value -= 10;
                        
                        availableMoves = Board.findKnightMoves(position, false, j, i);
                        
                        value -= (availableMoves-2)*2;
                        
                        break;
                    case 'b':
                        value += 40;
                        
                        value += (BISHOP_POSITION_VALUES[i][j]-0.35)*8;
                        
                        availableMoves = Board.findBishopMoves(position, true, j, i);
                        
                        value += availableMoves;
                        
                        break;
                    case 'B':
                        value -= 40;
                        
                        value -= (BISHOP_POSITION_VALUES[i][j]-0.35)*8;
                        
                        availableMoves = Board.findBishopMoves(position, false, j, i);
                        
                        value -= availableMoves;
                        
                        break;
                    case 'r':
                        value += 55;
                        
                        availableMoves = Board.findRookMoves(position, true, j, i);
                        
                        
                        value += availableMoves*0.5;
                        if (isEndgame > 50)
                            value += availableMoves;
                        
                        break;
                    case 'R':
                        value -= 55;
                        
                        availableMoves = Board.findRookMoves(position, false, j, i);
                        
                        
                        value -= availableMoves*0.5;
                        if (isEndgame > 50)
                            value -= availableMoves;
                        
                        break;
                    case 'q':
                        value += 95;
                        
                        availableMoves = Board.findRookMoves(position, true, j, i);
                        availableMoves += Board.findBishopMoves(position, true, j, i);
                        
                        if (isMiddlegame > 25)
                            value += availableMoves;
                        
                        //value += 20*CENTRALIZATION[i][j];
                        break;
                    case 'Q':
                        value -= 95;
                        
                        availableMoves = Board.findRookMoves(position, false, j, i);
                        availableMoves += Board.findBishopMoves(position, false, j, i);
                        
                        if (isMiddlegame > 25)
                            value -= availableMoves;
                        
                        //value -= 20*CENTRALIZATION[i][j];
                        break;
                    case 'k':     
                        
                        if ((j == 7 || j == 6) && isEndgame < 65)
                        {
                            value += 20;
                            
                            if (position[6][5] != 'p')
                                value -= 2;
                            if (position[6][6] != 'p')
                                value -= 2;
                            if (position[6][7] != 'p')
                                value -= 2;
                        }
                        else if ((j == 0 || j == 1 || j == 2) && isEndgame < 65)
                        {
                            value += 15;
                            
                            if (position[6][0] != 'p')
                                value -= 1.5;
                            if (position[6][1] != 'p')
                                value -= 1.5;
                            if (position[6][2] != 'p')
                                value -= 1.5;
                        }
                        else if (isEndgame < 65 && (position[8][3] == '0' || position[8][3] == '2'))
                        {
                            if (position[8][4] == '0' || position[8][4] == '2')
                                value += 4;
                            if (position[8][5] == '0' || position[8][5] == '2')
                                value += 6;
                        }
                         
                        if (isEndgame > 70)
                        {
                            value += CENTRALIZATION[i][j]*16;
                        }
                            
                        break;
                        
                    case 'K':
                        if ((j == 7 || j == 6) && isEndgame < 65)
                        {
                            value -= 20;
                            
                            if (position[1][5] != 'P')
                                value += 2;
                            if (position[1][6] != 'P')
                                value += 2;
                            if (position[1][7] != 'P')
                                value += 2;
                            
                        }
                        else if ((j == 0 || j == 1 || j == 2) && isEndgame < 65)
                        {
                            value -= 15;
                            
                            if (position[1][0] != 'p')
                                value += 1.5;
                            if (position[1][1] != 'p')
                                value += 1.5;
                            if (position[1][2] != 'p')
                                value += 1.5;
                            
                        }
                        else if (isEndgame < 65 && (position[8][3] == '0' || position[8][3] == '1'))
                        {
                            if (position[8][4] == '0' || position[8][4] == '1')
                                value -= 4;
                            if (position[8][5] == '0' || position[8][5] == '1')
                                value -= 6;
                        }
                        
                        if (isEndgame > 70)
                        {
                            value -= CENTRALIZATION[i][j]*16;
                        }
                        
                        break;
                }
            }
        }        
        
        
        return value;
    }
    
    private Move minmax(char[][] position, int depth, int maxDepth, boolean isWhite, double alpha, double beta, LinkedList<Move> legalMoves, boolean allowNull, long zobrist)
    {
        movesAnalyzed++;
        
        if (!abortSearch && (System.nanoTime()-searchStartTime)/1000000 > maxThinkTime)
        {
            abortSearch = true;
            System.out.println("Aborted " + (System.nanoTime()-searchStartTime)/1000000);
        }
        
        boolean nullMove;
        Move move = null;
        double moveValue;
        double bestValue = 1000;
        if (isWhite)
            bestValue = -1000;
                
        if (legalMoves == null)
        {
            if (depth <= 3)
                legalMoves = Board.getLegalMoves(position, isWhite, true);
            else
                legalMoves = Board.getLegalMoves(position, isWhite, false);
        }

        
        LinkedList<Move> captureMoves = new LinkedList<Move>();
        long[] previousPositions;
        
        //captureMoves = Board.getLegalMoves(position, isWhite);
        
        //allowNull = false;
        for (Move candidateMove : legalMoves)
        {
            //candidatePosition = Board.cloneBoard(position);
            nullMove = false;

            if (!nullMove)
            {
                boolean cutoff = true;
                boolean isRepeat = false;
                zobrist = Board.makeMove(position, candidateMove, false, 0);
                
                zobrist = Board.calculateZobrist(position);
                previousPositions = Board.getPreviousPositions();
                
                for (int i = 0; i < previousPositions.length; i++)
                {
                    if (zobrist == previousPositions[i])
                    {
                        candidateMove.setValue(0);
                        isRepeat = true;
                    }
                }
                
                if (!isRepeat)
                {
                    lastAnalyzed = candidateMove;
                    if (abortSearch)
                    {
                        candidateMove.setValue(0);
                        candidateMove.setAbortion();
                        Board.unmakeMove(position, candidateMove, 0);
                        return candidateMove;
                    }
                    
                    if (depth >= maxDepth)
                    {
                        if (false && isWhite == this.isWhite)
                        {
                            //captureMoves = Board.getCaptures(position, !isWhite);
                            //System.out.println(captureMoves.size());
                            //System.out.println(depth);
                            candidateMove.setValue(minmax(position, depth+1, maxDepth, !isWhite, alpha, beta, null, false, zobrist).getValue());
                        }
                        else
                            candidateMove.setValue(evaluateLeaf(position));
                    }
                    /*CAUSES HANGING PIECES AFTER e4 e6 d4 Nc6 d5, MAYBE FIX?
                    else if (allowNull && depth < maxDepth-30 && !Board.isInCheck(position) && isEndgame < 65)
                    {
                        //System.out.println(maxDepth);
                        char enPassant = position[8][0];
                        cutoff = false;
                        double eval;
                        position[8][0] = ' ';

                        if (isWhite)
                        {

                            eval = minmax(position, depth, maxDepth-3, !isWhite, beta, beta+1, null, false, zobrist).getValue();

                            cutoff = eval >= beta;
                        }
                        else
                        {
                            eval = minmax(position, depth, maxDepth-3, !isWhite, alpha-1, alpha, null, false, zobrist).getValue();

                            cutoff = eval <= alpha;
                        }
                        if (cutoff)
                        {
                            //System.out.println(depth);
                            candidateMove.setValue(eval);
                            move = candidateMove;
                            nullMove = true;
                            //System.out.println(depth);
                            //break;
                            //position[8][0] = enPassant;
                            //return candidateMove;
                        }

                        position[8][0] = enPassant;
                    }*/

                    else
                        candidateMove.setValue(minmax(position, depth+1, maxDepth, !isWhite, alpha, beta, null, true, zobrist).getValue());

                    if (!cutoff)
                        candidateMove.setValue(minmax(position, depth+1, maxDepth, !isWhite, alpha, beta, null, true, zobrist).getValue());
                }
                zobrist = Board.unmakeMove(position, candidateMove, 0);
            }
            
            if (isWhite) 
            {
                if (candidateMove.getValue() > bestValue || move == null) {
                    bestValue = candidateMove.getValue();
                    move = candidateMove;
                    /*
                    if (localPv.size() <= depth)
                    {
                        localPv.add(candidateMove);
                    }
                    else
                    {
                        localPv.set(depth, candidateMove);
                    }*/
                    
                    alpha = Math.max(alpha, bestValue);
                    if (alpha >= beta) {
                        break;
                    }
                }
            } 
            else 
            {
                if (candidateMove.getValue() < bestValue || move == null) {
                    bestValue = candidateMove.getValue();
                    move = candidateMove;
                    /*
                    if (localPv.size() <= depth)
                    {
                        localPv.add(candidateMove);
                    }
                    else
                        localPv.set(depth, candidateMove);
                    */
                    
                    
                    beta = Math.min(beta, bestValue);
                    if (alpha >= beta) {
                        break;
                    }

                }
            }

        }
        
        
        if (move == null)
        {
            move = new Move();
            int result = Board.checkWinner(position);
            
            switch (result)
            {
                case 2:
                    move.setValue(50000-depth);
                    break;
                case 1:
                    move.setValue(-50000+depth);
                    break;
                case 0:
                    move.setValue(0);
            }
        }
        
        //localPv.add(move);
        
        //principalVariation[depth] = move;
        
        return move;
    }
    
    @Override
    public Move getMoveDecision() {
        return move;
    }

    @Override
    public void startThinking() {
        isThinking = true;
        if (!thread.isAlive())
        {
            //System.out.println("Starting");
            thread.start();
            
            for (int i = 0; i < processingThreads.length; i++)
            {
                processingThreads[i].start();
            }
        }
        else
            moves++;
    }

    @Override
    public void stopThinking() {
        isThinking = false;
        move = null;
    }
    
    public void setClock(Timer clock)
    {
        this.clock = clock;
    }
    
    @Override
    public Move getPremove()
    {
        return null;
    }
    
    public void clearPremoves()
    {
        
    }
}
