/**
 * Author: Alexander Venezia
 * 
 * Basic chess game with a computer opponent
 * The opponent's AI is based on the minmax algorithm.
 */

package chess;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ComputerPlayer implements Player, Runnable {
    private class TranspositionElement
    {
        private double eval;
        private int depth;
        private TranspositionElement(int depth, double eval)
        {
            this.eval = eval;
            this.depth = depth;
        }
    }
    
    private final HashMap<Long, TranspositionElement> transpositionMap = new HashMap<>();
    private final int MAX_HASH_SIZE = 25000;
    private final LinkedList<Long> transpositionMapOrder = new LinkedList<>();
    
    private boolean isBook; //Not currently used, but 
    private double isOpening; //Degree to which computer thinks the game is in the opening stage
    private double isMiddlegame; //Same for middlegame
    private double isEndgame; //Same for endgame
    
    private boolean isThinking; //Whether the computer is currently determining its next move
    private Move move; //Move the computer has decided upon
    private int movesAnalyzed; //Metric for number of moves analyzed on the last move
    private boolean isWhite; //Whether the computer is white
    private final Thread thread; //Main thread the AI runs on
    
    //Number of additional threads to use for calculation. At zero, only the main thread is used for calculation. On some computers, performance may be improved by increasing this.
    //The advantage of increasing the number of threads is improved utilization of multicored CPUs. The disadvantage is that, by splitting up and desynchronizing the processing load, the algorithm's alpha-beta pruning is rendered less effective.
    private static final int PROCESSING_THREADS = 0; 
    
    private Thread[] processingThreads;
    private LinkedList<Move>[] processingThreadMoves;
    private Move[] processingThreadChoice;
    private boolean[] processingThreadsComplete;
    
    private int thinkTime; //Amount of time the computer will try to think about its next move. Actual think time will vary.
    private int maxThinkTime; //The absolute maximum amount of time the computer will spend on its next move. If exceeded, the next iteration of the move search will be aborted immediately and the best move determined so far will be returned.
    private int minThinkTime; //The absolute minimum amount of time the computer will spend on its next move. If the move search is complete before this number is exceeded, the computer will begin another iteration.
    private static final int EXPECTED_TIME_MULT = 30; //How much the computer expects each deeper iteration to take compared to the previous one.
    private static final int EXPECTED_TIME_MULT_ENDGAME = 25; //In the endgame, the multiplier is expected to be lower due to fewer possible branches per move.
    
    //private static Move[] principalVariation;
    private LinkedList<Move> principalVariation; //Not currently used. If implemented, this would allow the AI to return its expected variation for its chosen move.
    
    private int totalMovesAnalyzed; //Metric for number of moves analzyed across all moves
    private double totalTime; //Time spent on all moves
    
    private int moves;
    
    private int leaves; //Metric for number of leaves reached
    
    private boolean abortSearch; //Whether or not the search is to be immediately aborted
    
    private int threadCount = 0;
    
    private long searchStartTime;
    
    public Move lastAnalyzed;   
    
    //private static final int[] WINDOW_SIZES = new int[]{20, 20, 20, 15, 15, 15, 15}; // Not currently used
    
    private Timer clock;
    
    //Some pieces become more valuable the closer they are to the center of the board
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
    
    //Bishops have more specific squares where they are more valuable
    private static final double[][] BISHOP_POSITION_VALUES = new double[][]
    {
        {0.10, 0.10, 0.00, 0.10, 0.10, 0.00, 0.10, 0.10},
        {0.20, 1.25, 0.10, 0.30, 0.30, 0.10, 1.25, 0.20},
        {0.80, 0.80, 0.10, 0.35, 0.35, 0.10, 0.80, 0.80},
        {0.15, 0.30, 0.75, 0.20, 0.20, 0.75, 0.30, 0.15},
        {0.15, 0.30, 0.75, 0.20, 0.20, 0.75, 0.30, 0.15},
        {0.80, 0.80, 0.10, 0.35, 0.35, 0.10, 0.80, 0.80},
        {0.20, 1.25, 0.10, 0.30, 0.30, 0.10, 1.25, 0.20},
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
    
    //Linear interpolation
    private double lerp(double value1, double value2, double factor)
    {
        return (1-factor) * value1 + factor * value2;
    }
    
    @Override
    public void run()
    {
        int thisThread = threadCount;
        threadCount++;
        
        if (threadCount == 1) //If we're on the main thread
        {
            while (true)
            {
                if (isThinking && move == null) //If it's time for us to find a move, start looking
                {
                    char[][] position = Board.getBoardPosition();
                    long zobrist = Board.getZobrist();

                    LinkedList<Move> possibleMoves = Board.getLegalMoves(position, isWhite, true);

                    move = determineMove(position, possibleMoves, zobrist);
                }
                else //Otherwise, sleep briefly to avoid overutilizing computer resoruces
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

                        choice = minmax(position, 0, currentDepth, isWhite, alpha, beta, processingThreadMoves[thisThread-1], zobrist);
                        
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
    
    //Reorder move list to place current best move at the start
    private void reorderMoves(LinkedList<Move> legalMoves, Move bestMove)
    {
        legalMoves.remove(bestMove);
        legalMoves.add(0, bestMove);
    }
    
    //Determiens how long the computer will think on the next move, along with minimum and maximum time.
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
        
        int delay = clock.getDelay()*1000; //Find clock delay in milliseconds
        int increment = clock.getIncrement()*1000; //Find clock increment in milliseconds               
        
        thinkTime = (int)timeLeft/Math.max((30-moves), 5); //Calculate base think time
        
        //Determine multiplier for maximum time based on time remaining
        if (timeLeft < 60*1000)
            maxMult = 1.5; 
        else if (timeLeft < 180*1000)
            maxMult = 2;
        else if (timeLeft < 600*1000)
            maxMult = 2.5;
        else
            maxMult = 3;
        
        maxThinkTime = (int)(thinkTime*maxMult); //Determine maximum think time
        
        //If time is low, set max think time to be the same as think time
        if (timeLeft < 10*1000 && increment == 0 && delay == 0)
        {
            thinkTime = 500;
            maxThinkTime = 500;
            
            //If time is extremely low, lower think time and max think time
            if (timeLeft < 4*1000)
            {
                thinkTime = 200;
                maxThinkTime = 200;
            }
        }
        
        //If increment exists, think for longer
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
                if (maxThinkTime > increment)
                {
                    thinkTime = increment;
                    maxThinkTime = increment;
                }
            }
        }
            
        System.out.println("Target time: " + thinkTime/1000 + "; Minimum time: " + minThinkTime/1000 + "; Maximum time: " + maxThinkTime/1000 + "\n\n");
    }
    
    //Find next move
    private Move determineMove(char[][] position, LinkedList<Move> legalMoves, long zobrist)
    {       
        
        if (legalMoves.size() == 1) //If there is only one legal move, return that
            return legalMoves.get(0);
        
        determineThinkTime();
        
        long startTime = System.nanoTime(); //Time when we start looking for the move
        searchStartTime = startTime;
        int moveCount = 0;
        int remainder = 0;
        LinkedList<Move> mainMoves = new LinkedList<>(); //Moves to be analyzed by the main thread. If there is only one processing thread, this will include all legal moves.
        
        for (int i = 0; i < legalMoves.size()/(processingThreads.length+1); i++)
        {
            mainMoves.add(legalMoves.get(moveCount));
            moveCount++;
        }
        
        //Fill processing threads with their assigned moves, if they are enabled
        for (int i = 0; i < processingThreads.length; i++)
        {
            for (int j = 0; j < legalMoves.size()/(processingThreads.length+1); j++)
            {
                processingThreadMoves[i].add(legalMoves.get(moveCount));
                moveCount++;
            }
        }
        
        //Add any remaining moves to the main thread
        remainder = legalMoves.size()-moveCount;
        
        for (int i = 0; i < remainder; i++)
        {
            mainMoves.add(legalMoves.get(moveCount));
            moveCount++;
        }
        
        //If the processing threads have work to do, turn them on
        for (int i = 0; i < processingThreads.length; i++)
        {
            if (processingThreadMoves[i].size() == 0)
            {
                processingThreadsComplete[i] = true;
            }
        }
        
        //Reset metrics
        leaves = 0;
        movesAnalyzed = 0;
        
        Move choice = null;
        Move newChoice = null;
        
        
        double timeTaken = 0;
        
        int currentDepth = -1; //How many iterations of the iterative deepening search we have executed
        
        //Used for alpha beta pruning. These values are arbitrary; it is only important that they be outside of the possible spectrum of move evaluations.
        int alpha = -10000000;
        int beta = 10000000;
        
        //Determine estimated game state based on current position
        isOpening = isOpening(position);
        isEndgame = isEndgame(position);
        
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
                        
            newChoice = minmax(position, 0, currentDepth, isWhite, alpha, beta, mainMoves, zobrist);
            
            
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
        System.out.println("Transposition size: " + transpositionMap.size());
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
        
        //if (transpositionMap.size() > MAX_HASH_SIZE*0.75)
        //transpositionMap.clear();
        
        return choice;
    }
    
    //Counts the raw material of a given position (9 for queen, 5 for rook, 3 for knight and bishop, 1 for pawn)
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
    
    //Calculates how much raw material is on its starting square, used to estimate what stage the game is in
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
    
    //Performs a rough evaluation of a given position (a leaf of the search tree)
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
                        value += 17; //Base value of piece
                        value += CENTRALIZATION[i][j]*4; //Pawns are more valuable if centralized
                        value -= i*1; //More advanced pawns are more valuable
                        break;
                    case 'P':
                        value -= 17;
                        value -= CENTRALIZATION[i][j]*4;
                        value += 7-(i*1);
                        
                        break;
                    case 'n':
                        value += 35;
                        value += (CENTRALIZATION[i][j]-0.5)*5;
                        
                        //Knights on the fifth and sixth rank are particularly valuable
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
    
    /**
     * 
     * @param position Game state
     * @param depth How far we have searched already
     * @param maxDepth How deeply to search
     * @param isWhite Whether or not white has the move
     * @param alpha Minimum score the maximizing player is assured of
     * @param beta Maximum score the minimizing player is assured of
     * @param legalMoves List of all legal moves in a given position. If null, the list of legal moves will be determined
     * @param zobrist Zobrist hash of the current position
     * @return The move determined
     */
    private Move minmax(char[][] position, int depth, int maxDepth, boolean isWhite, double alpha, double beta, LinkedList<Move> legalMoves, long zobrist)
    {
        movesAnalyzed++;
        
        //If we're out of time, abort search
        if (!abortSearch && (System.nanoTime()-searchStartTime)/1000000 > maxThinkTime)
        {
            abortSearch = true;
            System.out.println("Aborted " + (System.nanoTime()-searchStartTime)/1000000);
        }
        
        Move move = null;
        
        //Initiate best value to an unreachably terrible score
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

        
        //LinkedList<Move> captureMoves = new LinkedList<Move>();
        HashSet<Long> previousPositions;
        
        //captureMoves = Board.getLegalMoves(position, isWhite);
        
        //allowNull = false;
        for (Move candidateMove : legalMoves)
        {
            //candidatePosition = Board.cloneBoard(position);


            boolean cutoff = true;
            boolean isRepeat = false;
            zobrist = Board.makeMove(position, candidateMove, false, 0); //Ideally, makeMove would return the correct zobrist value, however in the current implementation, it returns only a placeholder.

            zobrist = Board.calculateZobrist(position);
            previousPositions = Board.getPreviousPositions();
            
            /*
            for (int i = 0; i < previousPositions.length; i++)
            {
                if (zobrist == previousPositions[i])
                {
                    candidateMove.setValue(0);
                    isRepeat = true;
                }
            }*/
            
            boolean inTransposition = false;
            if (transpositionMap.containsKey(zobrist))
            {
                TranspositionElement element = transpositionMap.get(zobrist);
                
                if (element.depth >= maxDepth-depth && element.eval < 1000 && element.eval > -1000)
                {
                    inTransposition = true;
                    candidateMove.setValue(element.eval);
                    //System.out.println("Severed at " + depth);
                }
            }
            
            if (previousPositions.contains(zobrist))
            {
                candidateMove.setValue(0);
                isRepeat = true;
            }
            
            if (!inTransposition && !isRepeat)
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
                        candidateMove.setValue(minmax(position, depth+1, maxDepth, !isWhite, alpha, beta, null, zobrist).getValue());
                    }
                    else
                        candidateMove.setValue(evaluateLeaf(position));
                }                    

                else
                    candidateMove.setValue(minmax(position, depth+1, maxDepth, !isWhite, alpha, beta, null, zobrist).getValue());

                if (!cutoff)
                    candidateMove.setValue(minmax(position, depth+1, maxDepth, !isWhite, alpha, beta, null, zobrist).getValue());


                if (transpositionMap.size() < MAX_HASH_SIZE && maxDepth-depth > 2 && (maxDepth-depth)%2 == 1)
                {
                    if (!transpositionMap.containsKey(zobrist))
                    {
                        transpositionMapOrder.add(zobrist);
                        if (transpositionMapOrder.size() > MAX_HASH_SIZE)
                        {
                            long removed = transpositionMapOrder.removeFirst();
                            transpositionMap.remove(removed);
                        }
                    }
                    transpositionMap.put(zobrist, new TranspositionElement(maxDepth-depth, candidateMove.getValue()));                        
                }
            
            }
            zobrist = Board.unmakeMove(position, candidateMove, 0);
            
            
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
                    //transpositionMap.put(zobrist, new TranspositionElement(maxDepth-depth, move.getValue()));
                    break;
                case 1:
                    move.setValue(-50000+depth);
                    //transpositionMap.put(zobrist, new TranspositionElement(maxDepth-depth, move.getValue()));
                    break;
                case 0:
                    move.setValue(0);
                    //transpositionMap.put(zobrist, new TranspositionElement(maxDepth-depth, move.getValue()));
                    break;
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
    
    @Override
    public void setClock(Timer clock)
    {
        this.clock = clock;
    }
    
    @Override
    public Move getPremove()
    {
        return null;
    }
    
    public void clearPremoves() {} //Intentionally empty
    
    public boolean isHuman()
    {
        return false;
    }
}
