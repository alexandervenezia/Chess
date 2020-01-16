/**
 * Author: Alexander Venezia
 * 
 * Basic chess game with a computer opponent
 * The opponent's AI is based on the minmax algorithm.
 */

package chess;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;


public class Board {
    private static final int SQUARE_SIZE = 110; //Size in pixels of each square of the board. This value can be changed freely, although there maybe problems if it is too large or too small.
    
    //Time for each player in minutes/seconds for convenience
    private static final int WHITE_TIME_SECONDS = 3; //White's time in seconds, should be 0-60
    private static final int WHITE_TIME_MINUTES = 5; //White's time in minutes
    
    private static final int BLACK_TIME_SECONDS =  3;
    private static final int BLACK_TIME_MINUTES = 5;
    
    //Calculate white and black's time in milliseconds
    private static final int WHITE_START_TIME = (WHITE_TIME_MINUTES*60 + WHITE_TIME_SECONDS)*1000;
    private static final int BLACK_START_TIME = (BLACK_TIME_MINUTES*60 + BLACK_TIME_SECONDS)*1000;
    private static final int INCREMENT = 0; //Seconds added to a player's clock every time they make a move
    private static final int DELAY = 0; //Seconds before a player begins to lose time
    
    private static Timer clock; //Chess clock to enforce maximum time per move
    
    private static final Font CLOCK_FONT = new java.awt.Font("Courier New", java.awt.Font.BOLD, 40);
    private static final Font GAME_OVER_FONT = new java.awt.Font("Courier New", java.awt.Font.BOLD, 100);
    
    //Colors of chessboard
    private static final Color LIGHT_COL = new Color(210, 190, 235);
    private static final Color DARK_COL = new Color(90, 75, 100);
    
    private static final Color SELECTED_COL = new Color(150, 175, 225); //Color of selected square
    private static final Color PREMOVE_COL = new Color(150, 175, 120); //Color of square which involves a premove
    private static final Color FROM_COL = new Color(40, 125, 125); //Color of a square from which a piece was moved
    private static final Color TO_COL = new Color(140, 225, 225); //Color of square to which a piece was moved
    
    private static final Color PANEL_COL = new Color(40, 60, 150);
    
    private static final BufferedImage PIECES_IMAGE = loadImage(Chess.class.getResourceAsStream("ChessPieces.png")); //The image which contains all piece icons
    
    //Get subimages
    private static final Image WHITE_KING = PIECES_IMAGE.getSubimage(0, 0, 167, 167).getScaledInstance(SQUARE_SIZE, SQUARE_SIZE, Image.SCALE_SMOOTH);
    private static final Image WHITE_QUEEN = PIECES_IMAGE.getSubimage(167, 0, 167, 167).getScaledInstance(SQUARE_SIZE, SQUARE_SIZE, Image.SCALE_SMOOTH);
    private static final Image WHITE_BISHOP = PIECES_IMAGE.getSubimage(334, 0, 167, 167).getScaledInstance(SQUARE_SIZE, SQUARE_SIZE, Image.SCALE_SMOOTH);
    private static final Image WHITE_KNIGHT = PIECES_IMAGE.getSubimage(501, 0, 167, 167).getScaledInstance(SQUARE_SIZE, SQUARE_SIZE, Image.SCALE_SMOOTH);
    private static final Image WHITE_ROOK = PIECES_IMAGE.getSubimage(668, 0, 167, 167).getScaledInstance(SQUARE_SIZE, SQUARE_SIZE, Image.SCALE_SMOOTH);
    private static final Image WHITE_PAWN = PIECES_IMAGE.getSubimage(835, 0, 167, 167).getScaledInstance(SQUARE_SIZE, SQUARE_SIZE, Image.SCALE_SMOOTH);
    
    private static final Image BLACK_KING = PIECES_IMAGE.getSubimage(0, 167, 167, 167).getScaledInstance(SQUARE_SIZE, SQUARE_SIZE, Image.SCALE_SMOOTH);
    private static final Image BLACK_QUEEN = PIECES_IMAGE.getSubimage(167, 167, 167, 167).getScaledInstance(SQUARE_SIZE, SQUARE_SIZE, Image.SCALE_SMOOTH);
    private static final Image BLACK_BISHOP = PIECES_IMAGE.getSubimage(334, 167, 167, 167).getScaledInstance(SQUARE_SIZE, SQUARE_SIZE, Image.SCALE_SMOOTH);
    private static final Image BLACK_KNIGHT = PIECES_IMAGE.getSubimage(501, 167, 167, 167).getScaledInstance(SQUARE_SIZE, SQUARE_SIZE, Image.SCALE_SMOOTH);
    private static final Image BLACK_ROOK = PIECES_IMAGE.getSubimage(668, 167, 167, 167).getScaledInstance(SQUARE_SIZE, SQUARE_SIZE, Image.SCALE_SMOOTH);
    private static final Image BLACK_PAWN = PIECES_IMAGE.getSubimage(835, 167, 167, 167).getScaledInstance(SQUARE_SIZE, SQUARE_SIZE, Image.SCALE_SMOOTH);
    
    private static final Image POSSIBLE_MOVE = loadImage(Chess.class.getResourceAsStream("MoveIndicator.png"));
    
    private static final HashMap PIECES = new HashMap<Character, Image>(); //Maps character representation of chess pieces to their images
    
    private static final HumanPlayer player1 = new HumanPlayer(true);
    //private static final ComputerPlayer player1 = new ComputerPlayer(true);
    private static final ComputerPlayer player2 = new ComputerPlayer(false);
    //private static final HumanPlayer player2 = new HumanPlayer(false);
    
    private static Point selected = new Point(-1, -1); //Currently selected square
    private static Point movedFrom = new Point(-1, -1); //Square a piece moved from
    private static Point movedTo = new Point(-1, -1); //Square a piece moved to
    
    private static boolean isWhiteTurn; //Whether white has one or not
    private static boolean isGameOver;
    private static boolean playerOneWon;
    private static boolean playerTwoWon;
    
    private static char nextPromotion = 'q'; //What the next pawn that reaches the 8th rank will promote to
    
    private static int reachedPositionsIndex = 0; //Where in reachedPositions to store next game position
    //private static final long[] reachedPositions = new long[30]; //Stores zobrist hashes of previous positions to detect 3-fold repetition. The value of 30 is abritrary, however it is exceedingly unlikely to reach 3-fold repetition over a span that large and would likely be missed in an OTB game anyway.
    private static final HashSet<Long> reachedPositions = new HashSet<>();
    
    private static final int[] POSITION = {120, 25}; //Position of main game board
    private static final int[] BLACK_CLOCK_POSITION = {1020, 430}; //Position of black's clock
    private static final int[] WHITE_CLOCK_POSITION = {1020, 520}; //Position of white's clock
    private static final int[] PROMOTION_PANEL_POSITION = {0, 225};
    
    private static final boolean FLIP_COLORS = false; //Not properly implemented yet
    
    private static String moveLog = ""; //List of all moves played in algebraic notation, displayed to the console after every move
    
    private boolean allowPremoves = true; //Allows the human player to choose a move while the computer is deliberating, which will be played immediately after the computer's move, if it is legal
    private boolean blindfold = false; //If set to true, no pieces are rendered to challenge the human's memory.
    
    private static final char[][] BOARD = 
    {
        {'R', 'N' , 'B', 'Q', 'K', 'B', 'N', 'R'},
        {'P', 'P', 'P', 'P', 'P', 'P', 'P', 'P'},
        {' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '},
        {' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '},
        {' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '},
        {' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '},
        {'p', 'p', 'p', 'p', 'p', 'p', 'p', 'p'},
        {'r', 'n', 'b', 'q', 'k', 'b', 'n', 'r'},
        {' ', '0', '0', '0', '0', '0', ' ', ' '} //Last line represents meta information. The first entry is for en-passant captures. Blank for no capture, number 0-7 to represent file of possible en-passant capture. Up to two pawns can execute an en-passant capture, but only one pawn may be thus captured.
            //The second entry is for the number of repeated moves. The third entry is for the number of moves since capture or pawn move. The fourth entry is for whether or not the king has moved, 0 for neither king having moved, 1 for the white king only, 2 for the black king only, 3 for both kings
            //The fifth and sixth are for whether the rooks have moved, queenside and kingside respectively. These work the same as the king. The seventh and eighth entries are placeholders
            //and currently have no meaning.
    };

    
    public static final char[][] STARTING_POSITION = cloneBoard(BOARD);
    
    /*
    A zobrist hash allows you to represent a given game position in a single long, significantly less space than the standard nested array. Rarely, two positions may have the same zobrist hash. However, these scenarios are unlikely enough to have minimal impact.
    A zobrist hash is formed by a series of xor operations. Each piece has a different predetermined random value for each possible position. All values for each piece are xored, producing a unique number.
    Castling rights and en passant are also "baked into" the zobrist value, by xoring different values based on the state of castling rights and en passant.
    */
    
    private static long[][][] zobristPieces = new long[12][8][8]; //All pieces, black and white, for each possible position
    private static long [] zobristCastlingWhite = new long[4]; //White's castling rights
    private static long [] zobristCastlingBlack = new long[4]; //Black's castling rights
    private static long[] zobristEnPassant = new long[8]; //En passant zobrist values
    
    private static long currentZobrist; //The zobrist hash of the current position
    
    public Board(Display display)
    {
        //Associate string abbreviations of pieces with their images
        PIECES.put('p', WHITE_PAWN);
        PIECES.put('n', WHITE_KNIGHT);
        PIECES.put('b', WHITE_BISHOP);
        PIECES.put('r', WHITE_ROOK);
        PIECES.put('q', WHITE_QUEEN);
        PIECES.put('k', WHITE_KING);
        
        PIECES.put('P', BLACK_PAWN);
        PIECES.put('N', BLACK_KNIGHT);
        PIECES.put('B', BLACK_BISHOP);
        PIECES.put('R', BLACK_ROOK);
        PIECES.put('Q', BLACK_QUEEN);
        PIECES.put('K', BLACK_KING);
        
        
        
        display.addMouseListener(player1);
        //display.addMouseListener(player2);
       
        clock = new Timer(WHITE_START_TIME, BLACK_START_TIME, INCREMENT, DELAY);
        
        //player1.setClock(clock);
        player2.setClock(clock);
        
        isWhiteTurn = true;
        
        if (isWhiteTurn)
            player1.startThinking();
        else
            player2.startThinking();
        
        isGameOver = false;
        playerOneWon = false;
        playerTwoWon = false;
        
        initiateZobrist(); //Generate zobrist values and calculate the initial zobrist hash
        
        
        
        
        
    }
    
    //Generates random values for zobrist values and initiates hash for starting position
    private static void initiateZobrist()
    {
        Random rand = new Random();
        
        for (int i = 0; i < 4; i++)
        {
            zobristCastlingWhite[i] = rand.nextLong();
            zobristCastlingBlack[i] = rand.nextLong();
        }
        
        for (int i = 0; i < 8; i++)
        {
            zobristEnPassant[i] = rand.nextLong();
        }
        
        for (int i = 0; i < 8; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                for (int k = 0; k < 12; k++)
                {
                    zobristPieces[k][j][i] = rand.nextLong();
                }
            }
        }
        
        currentZobrist = calculateZobrist(BOARD);
    }
    
    //Calculates the zobrist hash for a given position.
    public static long calculateZobrist(char[][] position)
    {
        boolean whiteCastleQueenside = true, whiteCastleKingside = true, blackCastleQueenside = true, blackCastleKingside = true; //Castling rights
        long zobrist = 0; //Zobrist value
        char piece;
        
        //Modify zobrist hash for each piece
        for (int i = 0; i < 8; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                piece = position[i][j];
                
                switch (piece)
                {
                    case 'p':
                        zobrist ^= zobristPieces[0][i][j];
                        break;
                    case 'n':
                        zobrist ^= zobristPieces[1][i][j];
                        break;
                    case 'b':
                        zobrist ^= zobristPieces[2][i][j];
                        break;
                    case 'r':
                        zobrist ^= zobristPieces[3][i][j];
                        break;
                    case 'q':
                        zobrist ^= zobristPieces[4][i][j];
                        break;
                    case 'k':
                        zobrist ^= zobristPieces[5][i][j];
                        break;
                    case 'P':
                        zobrist ^= zobristPieces[6][i][j];
                        break;
                    case 'N':
                        zobrist ^= zobristPieces[7][i][j];
                        break;
                    case 'B':
                        zobrist ^= zobristPieces[8][i][j];
                        break;
                    case 'R':
                        zobrist ^= zobristPieces[9][i][j];
                        break;
                    case 'Q':
                        zobrist ^= zobristPieces[10][i][j];
                        break;
                    case 'K':
                        zobrist ^= zobristPieces[11][i][j];
                        break;
                }                
            }
        }
        
        //Determine castling rights
        switch (position[8][3])
        {
            case '1':
                whiteCastleQueenside = false;
                whiteCastleKingside = false;
                break;
            case '2':
                blackCastleQueenside = false;
                blackCastleKingside = false;
                break;
            case '3':
                whiteCastleQueenside = false;
                whiteCastleKingside = false;

                blackCastleQueenside = false;
                blackCastleKingside = false;
                break;
        }

        switch (position[8][4]) {
            case '1':
                whiteCastleQueenside = false;
                break;
            case '2':
                blackCastleQueenside = false;
                break;
            case '3':
                whiteCastleQueenside = false;
                blackCastleQueenside = false;
                break;
        }

        switch (position[8][5]) {
            case '1':
                whiteCastleKingside = false;
                break;
            case '2':
                blackCastleKingside = false;
                break;
            case '3':
                whiteCastleKingside = false;
                blackCastleKingside = false;
                break;
        }
        
        
        //Modify zobrist by castling rights
        if (whiteCastleKingside && whiteCastleQueenside) {
            zobrist ^= zobristCastlingWhite[0];
        } else if (whiteCastleKingside && !whiteCastleQueenside) {
            zobrist ^= zobristCastlingWhite[1];
        } else if (!whiteCastleKingside && whiteCastleQueenside) {
            zobrist ^= zobristCastlingWhite[2];
        } else {
            zobrist ^= zobristCastlingWhite[3];
        }

        if (blackCastleKingside && blackCastleQueenside) {
            zobrist ^= zobristCastlingBlack[0];
        } else if (blackCastleKingside && !blackCastleQueenside) {
            zobrist ^= zobristCastlingBlack[1];
        } else if (!blackCastleKingside && blackCastleQueenside) {
            zobrist ^= zobristCastlingBlack[2];
        } else {
            zobrist ^= zobristCastlingBlack[3];
        }
        
        //Modify zobrist by en passant
        if (position[8][0] != ' ')
        {
            zobrist ^= zobristEnPassant[Character.getNumericValue(position[8][0])];
        }
        
        return zobrist;
    }
    
    //Updates the game, checking if new moves have been determined
    public void update(double delta)
    {
        if (!isGameOver)
        {
            Move move; //Potential new move
            
            if (clock.getWhiteTime() < 0) //Check if white has timed out
            {
                isGameOver = true;
                playerOneWon = false; //TODO: Implement draws by insufficient material in case of timeouts
                playerTwoWon = true;
                clock.stop();
                System.out.println("Black wins.");
            }
            else if (clock.getBlackTime() < 0) //Check if black has timed out
            {
                isGameOver = true;
                playerOneWon = true;
                playerTwoWon = false;
                clock.stop();
                System.out.println("White wins.");
            }
                
            if (player1.getMoveDecision() != null && isWhiteTurn) //If it is white's turn and they've decided on their move
            {
                if (!clock.isRunning())
                    clock.start();
                
                clock.flip(); //Stop white's clock and start black's
                
                move = player1.getMoveDecision(); //Get white's move
                move.setMovingPiece(BOARD[move.getStartSquare().y][move.getStartSquare().x]);
                
                moveLog += move.getAlgebraic() + " ";
                
                currentZobrist = makeMove(BOARD, move, true, currentZobrist);
                
                player1.stopThinking(); //Tell player 1 to stop thinking about their next move
                
                if (!isGameOver)
                    player2.startThinking();
                isWhiteTurn = !isWhiteTurn;
            }
            else if (player2.getMoveDecision() != null && !isWhiteTurn) //Black's turn and they've made a move
            {
                if (!clock.isRunning())
                    clock.start();
                
                clock.flip();
                
                move = player2.getMoveDecision();
                move.setMovingPiece(BOARD[move.getStartSquare().y][move.getStartSquare().x]);
                
                moveLog += move.getAlgebraic() + " ";
                
                currentZobrist = makeMove(BOARD, move, true, currentZobrist);

                player2.stopThinking();
                
                if (!isGameOver)
                    player1.startThinking();
                
                if (player1.getPremove() != null && allowPremoves) //We only check for premoves by white, the human player
                {
                    Move premove = player1.getPremove();
                    
                    if (Board.isLegalMove(premove, !isWhiteTurn)) //Check if premove is a legal move, and if it is, make the move
                    {
                        currentZobrist = makeMove(BOARD, premove, true, currentZobrist);
                        isWhiteTurn = !isWhiteTurn;
                        clock.flip();
                        player1.stopThinking();
                        player2.startThinking();
                    }
                    else
                        player1.clearPremoves();
                }
                
                isWhiteTurn = !isWhiteTurn;
            }
        }
    }
    
    //Renders all elements of the game board
    public void render(Graphics g)
    {
        boolean darkSquare = false; //Used for alternating between light squares and dark squares on chessboard
        
        for (int i = 0; i < 8; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                g.setColor(darkSquare ? DARK_COL : LIGHT_COL);
                
                if (j == movedFrom.x && i == movedFrom.y)
                    g.setColor(FROM_COL);
                else if (j == movedTo.x && i == movedTo.y)
                    g.setColor(TO_COL);
                
                if (j == selected.x && i == selected.y)
                    g.setColor(SELECTED_COL);
                
                if (player1.getPremove() != null && allowPremoves) //Render premove highlight if applicable
                {
                    if (j == player1.getPremove().getStartSquare().x && i == player1.getPremove().getStartSquare().y)
                        g.setColor(PREMOVE_COL);
                    else if (j == player1.getPremove().getEndSquare().x && i == player1.getPremove().getEndSquare().y)
                        g.setColor(PREMOVE_COL);
                }
                
                g.fillRect(POSITION[0]+j*SQUARE_SIZE, POSITION[1]+i*SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
                
                if (!blindfold) //Render piece at board position if blindfold isn't enabled
                    renderPiece(g, BOARD[i][j], POSITION[0]+j*SQUARE_SIZE, POSITION[1]+i*SQUARE_SIZE);
                
                darkSquare = !darkSquare;
            }
            darkSquare = !darkSquare;
        }
        
        if (selected.x != -1 && selected.y != -1)
        {
            renderPossibleMoves(g, selected); //Render possible moves of selected piece
        }
        
        g.setColor(Color.GRAY);
        g.fillRect(BLACK_CLOCK_POSITION[0]-5, BLACK_CLOCK_POSITION[1]-50, 300, 175);
        
        g.setFont(CLOCK_FONT);
        g.setColor(Color.WHITE);
        
        double time = clock.getWhiteTime();
        
        //Calculate minutes and seconds
        int m = (int)time/60;
        double s = time%60;
        
        if (s > 59)
        {
            s = 0;
            m++;
        }
        
        String minutes = String.format("%d", m);
        String seconds = String.format("%.0f", s);
        
        if (time < 10)
            seconds = String.format("%.1f", time%60);
                
        if (seconds.length() == 1 || seconds.length() == 3)
        {
            seconds = "0" + seconds;
        }
        
        g.drawString(minutes+":"+seconds, WHITE_CLOCK_POSITION[0], WHITE_CLOCK_POSITION[1]); //Render clock time
        
        time = clock.getBlackTime();
        
        m = (int)time/60;
        s = time%60;
        
        if (s > 59)
        {
            s = 0;
            m++;
        }
        
        minutes = String.format("%d", m);
        seconds = String.format("%.0f", s);
        
        if (time < 10)
            seconds = String.format("%.1f", time%60);
        
        if (seconds.length() == 1 || seconds.length() == 3)
        {
            seconds = "0" + seconds;
        }
        
        g.setColor(Color.BLACK);
        
        g.drawString(minutes+":"+seconds, BLACK_CLOCK_POSITION[0], BLACK_CLOCK_POSITION[1]);
        
        //Draw panel for selecting promotions

         g.setColor(PANEL_COL);
         g.fillRect(PROMOTION_PANEL_POSITION[0], PROMOTION_PANEL_POSITION[1], SQUARE_SIZE, 500);
         
         g.setColor(SELECTED_COL);
         
         switch (nextPromotion)
         {
             case 'q':
                 g.fillRect(PROMOTION_PANEL_POSITION[0], PROMOTION_PANEL_POSITION[1]+SQUARE_SIZE*3+20, SQUARE_SIZE, SQUARE_SIZE);
                 break;
             case 'r':
                 g.fillRect(PROMOTION_PANEL_POSITION[0], PROMOTION_PANEL_POSITION[1]+SQUARE_SIZE*2+20, SQUARE_SIZE, SQUARE_SIZE);
                 break;
             case 'b':
                 g.fillRect(PROMOTION_PANEL_POSITION[0], PROMOTION_PANEL_POSITION[1]+SQUARE_SIZE*1+20, SQUARE_SIZE, SQUARE_SIZE);
                 break;
             case 'n':
                 g.fillRect(PROMOTION_PANEL_POSITION[0], PROMOTION_PANEL_POSITION[1]+SQUARE_SIZE*0+20, SQUARE_SIZE, SQUARE_SIZE);
                 break;
         }
         
         //Draw promotion options
         g.drawImage(WHITE_KNIGHT, PROMOTION_PANEL_POSITION[0], PROMOTION_PANEL_POSITION[1]+SQUARE_SIZE*0+20, null);
         g.drawImage(WHITE_BISHOP, PROMOTION_PANEL_POSITION[0], PROMOTION_PANEL_POSITION[1]+SQUARE_SIZE*1+20, null);
         g.drawImage(WHITE_ROOK, PROMOTION_PANEL_POSITION[0], PROMOTION_PANEL_POSITION[1]+SQUARE_SIZE*2+20, null);
         g.drawImage(WHITE_QUEEN, PROMOTION_PANEL_POSITION[0], PROMOTION_PANEL_POSITION[1]+SQUARE_SIZE*3+20, null);
         
         
        g.setFont(GAME_OVER_FONT);
        if (isGameOver)
        {
            if (playerOneWon)
            {
                g.setColor(Color.WHITE);
                g.drawString("White wins", 325, 500);
            }
            else if (playerTwoWon)
            {
                g.setColor(Color.BLACK);
                g.drawString("Black wins", 325, 500);
            }
            
            else
            {
                g.setColor(Color.GRAY);
                g.drawString("Game is drawn", 325, 500);
            }
            
        }
    }
    
    //Render possible moves for a given piece. Note that moves that would be illegal due to pinning, check etc are still rendered.
    private void renderPossibleMoves(Graphics g, Point piece)
    {
        LinkedList<Move> possibleMoves = new LinkedList<>();
        
        char character = Character.toLowerCase(BOARD[piece.y][piece.x]);
        
        switch (character)
        {
            case 'p':
                findPawnMoves(BOARD, Character.isLowerCase(BOARD[piece.y][piece.x]), piece.x, piece.y, possibleMoves);
                break;
            case 'n':
                findKnightMoves(BOARD, Character.isLowerCase(BOARD[piece.y][piece.x]), piece.x, piece.y, possibleMoves);
                break;
            case 'b':
                findBishopMoves(BOARD, Character.isLowerCase(BOARD[piece.y][piece.x]), piece.x, piece.y, possibleMoves);
                break;
            case 'r':
                findRookMoves(BOARD, Character.isLowerCase(BOARD[piece.y][piece.x]), piece.x, piece.y, possibleMoves);
                break;
            case 'q':
                findBishopMoves(BOARD, Character.isLowerCase(BOARD[piece.y][piece.x]), piece.x, piece.y, possibleMoves);
                findRookMoves(BOARD, Character.isLowerCase(BOARD[piece.y][piece.x]), piece.x, piece.y, possibleMoves);
                break;
            case 'k':
                findKingMoves(BOARD, Character.isLowerCase(BOARD[piece.y][piece.x]), piece.x, piece.y, possibleMoves);
                break;
        }
        
        for (Move move : possibleMoves)
        {
            g.drawImage(POSSIBLE_MOVE, fromBoardCoords(move.getEndSquare()).x - POSSIBLE_MOVE.getWidth(null)/2, fromBoardCoords(move.getEndSquare()).y - POSSIBLE_MOVE.getHeight(null)/2, null);
        }
    }
    
    //Renders a piecec given a char representation
    private void renderPiece(Graphics g, char pieceCode, int x, int y)
    {
        if (FLIP_COLORS)
        {
            if (Character.isUpperCase(pieceCode))
                pieceCode = Character.toLowerCase(pieceCode);
            else
                pieceCode = Character.toUpperCase(pieceCode);
        }
        
        g.drawImage((Image) PIECES.get(pieceCode), x, y, null);
        if (pieceCode != ' ')
        {
            if (PIECES.get(pieceCode) == null)
                System.out.println(pieceCode);
        }
    }
    
    //Loads an image from disk
    private static BufferedImage loadImage(InputStream stream)
    {
        try {
            return ImageIO.read(stream);
        } catch (IOException ex) {
            Logger.getLogger(Board.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public static char[][] getBoardPosition()
    {
        return cloneBoard(BOARD);
    }
    
    //Performs a deep copy of a board
    public static char[][] cloneBoard(char[][] board)
    {
        char[][] newBoard = new char[9][8];
        
        for (int i = 0; i < 9; i++)
        {
            System.arraycopy(board[i], 0, newBoard[i], 0, board[i].length);
        }
        
        return newBoard;
    }
    
    //Sets square as selected by player
    public static void setSelected(Point selected)
    {
        if (onBoard(selected))
        {
            int x = toBoardCoords(selected).x;
            int y = toBoardCoords(selected).y;

            if (x == Board.selected.x && y == Board.selected.y)
                Board.selected.setLocation(-1, -1);
            else
                Board.selected.setLocation(x, y);
            
        }
    }
    
    //Verifies if a given move is legal
    public static boolean isLegalMove(Move move, boolean isWhite)
    {
        LinkedList<Move> moves = getLegalMoves(BOARD, isWhite, false);
        
        for (Move m : moves)
        {
            if (m.equals(move))
            {
                return true;
            }
        }
        return false;
    }
    
    //Unmakes a move. This is used for optimization reasons because it is more efficient to make and unmake a move than to perform a duplication of the chessboard and then only make a move.
    public static long unmakeMove(char[][] position, Move move, long zobrist)
    {
        char endPosPiece = position[move.getEndSquare().y][move.getEndSquare().x]; //Piece that was moved
        position[move.getStartSquare().y][move.getStartSquare().x] = endPosPiece; //Unmove the piece
        position[move.getEndSquare().y][move.getEndSquare().x] = move.getCapturedPiece(); //Replace the captured piece, if applicable.
        
        if (move.isFirstKingMove()) //If it was the first time a king was moved, restore castling rights
        {
            if (endPosPiece == 'k')
            {
                if (position[8][3] == '3')
                    position[8][3] = '2';
                else if (position[8][3] == '1')
                    position[8][3] = '0';
            }
            else if (endPosPiece == 'K')
            {
                if (position[8][3] == '3')
                    position[8][3] = '1';
                else if (position[8][3] == '2')
                    position[8][3] = '0';
            }
        }
        
        if (move.isFirstRookMove()) //Also restore castling rights for first rook moves
        {
            if (endPosPiece == 'r')
            {
                if (move.getStartSquare().x == 7) //Kingside rook
                {
                    //System.out.println("K");
                    if (position[8][5] == '1')
                        position[8][5] = '0';
                    if (position[8][5] == '3')
                        position[8][5] = '2';
                }
                else if (move.getStartSquare().x == 0) //Queenside rook
                {
                    if (position[8][4] == '1')
                        position[8][4] = '0';
                    if (position[8][4] == '3')
                        position[8][4] = '2';
                }
            }
            else if (endPosPiece == 'R')
            {
                if (move.getStartSquare().x == 7) //Kingside rook
                {
                    if (position[8][5] == '2')
                        position[8][5] = '0';
                    if (position[8][5] == '3')
                        position[8][5] = '1';
                }
                else if (move.getStartSquare().x == 0) //Queenside rook
                {
                    if (position[8][4] == '2')
                        position[8][4] = '0';
                    if (position[8][4] == '3')
                        position[8][4] = '1';
                }
            }
        }
        
        //If the move was a castle, we must also move the rook back
        int castle = move.getCastleVal();
        
        if (castle != 0)
        {
            if (castle == 1) //Kingside
            {
                char rook = position[move.getEndSquare().y][5];
                position[move.getEndSquare().y][5] = ' ';
                position[move.getEndSquare().y][7] = rook;
            }
            else if (castle == -1) //Queenside
            {
                char rook = position[move.getEndSquare().y][3];
                position[move.getEndSquare().y][3]= ' ';
                position[move.getEndSquare().y][0] = rook;
            }
        }
        
        //If the move was a promotion, undo that
        if (move.isPromotion())
        {
            if (move.getEndSquare().y == 0)
            {
                position[move.getStartSquare().y][move.getStartSquare().x] = 'p';
            }
            else if (move.getEndSquare().y == 7)
            {
                position[move.getStartSquare().y][move.getStartSquare().x] = 'P';
            }
        }
        
        //Handle en passant
        if (move.isEnPassant())
        {            
            if (move.getEndSquare().y == 2)
            {
                //System.out.println("White passant");
                position[3][move.getEndSquare().x] = 'P';
            }
            else if (move.getEndSquare().y == 5)
            {
                //System.out.println("Black passant");
                position[4][move.getEndSquare().x] = 'p';
            }
            else
                System.out.println("ERR");
        }
        
        //System.out.println(move.getEnPassantVal());
        position[8][0] = move.getEnPassantVal();
        
        return zobrist;
    }
    
    //Makes a move. Unmade by unmakeMove()
    public static long makeMove(char[][] position, Move move, boolean actual, long zobrist)
    {
        char startPiece = position[move.getStartSquare().y][move.getStartSquare().x]; //Piece that will be moved
        
        move.setEnPassantVal(position[8][0]);
       
        if (startPiece == 'k')
        {
            if (position[8][3] == '0')
            {
                position[8][3] = '1';
                move.setFirstKingMove(true);
            }
            else if (position[8][3] == '2') 
            {
                position[8][3] = '3';
                move.setFirstKingMove(true);
            }
        }
        else if (startPiece == 'K')
        {
            if (position[8][3] == '0')
            {
                position[8][3] = '2';
                move.setFirstKingMove(true);
            }
            else if (position[8][3] == '1')
            {
                position[8][3] = '3';
                move.setFirstKingMove(true);
            }
        }
        
        if (startPiece == 'r')
        {
            if (move.getStartSquare().x == 7) //Kingside rook
            {
                if (position[8][5] == '0')
                {
                    position[8][5] = '1';
                    move.setFirstRookMove(true);
                }
                if (position[8][5] == '2')
                {
                    position[8][5] = '3';
                    move.setFirstRookMove(true);
                }
            }
            else if (move.getStartSquare().x == 0) //Queenside
            {
                if (position[8][4] == '0')
                {
                    position[8][4] = '1';
                    move.setFirstRookMove(true);
                }
                if (position[8][4] == '2')
                {
                    position[8][4] = '3';
                    move.setFirstRookMove(true);
                }
            }
        }
        
        if (startPiece == 'R')
        {
            if (move.getStartSquare().x == 7) //Kingside rook
            {
                if (position[8][5] == '0')
                {
                    position[8][5] = '2';
                    move.setFirstRookMove(true);
                }
                if (position[8][5] == '1')
                {
                    position[8][5] = '3';
                    move.setFirstRookMove(true);
                }
            }
            else if (move.getStartSquare().x == 0) //Queenside
            {
                if (position[8][4] == '0')
                {
                    position[8][4] = '2';
                    move.setFirstRookMove(true);
                }
                if (position[8][4] == '1')
                {
                    position[8][4] = '3';
                    move.setFirstRookMove(true);
                }
            }
        }
        
        
        boolean enPassant = false;
        int castle = 0;
        
        if (move.getStartSquare().x != move.getEndSquare().x && position[move.getEndSquare().y][move.getEndSquare().x] == ' ' && (startPiece == 'p' || startPiece == 'P'))
        {
            enPassant = true;
        }
        
        if (startPiece == 'k' || startPiece == 'K')
        {
            if (Math.abs(move.getStartSquare().x-move.getEndSquare().x) > 1)
            {
                if (move.getEndSquare().x > move.getStartSquare().x)
                    castle = 1; //Kingside castle
                else
                    castle = -1; //Queenside
                move.setCastleVal(castle);
            }
        }
        
        if (enPassant)
        {
            move.setEnPassant(true);
            if (move.getEndSquare().y == 2)
            {
                if (position[3][move.getEndSquare().x] == ' ')
                    System.out.println(Arrays.deepToString(position));

                    //System.out.println("EXRRR " + position[3][move.getEndSquare().x] + " " + position[move.getEndSquare().y][move.getEndSquare().x]);
                position[3][move.getEndSquare().x] = ' ';
            }
            else if (move.getEndSquare().y == 5)
            {
                if (position[4][move.getEndSquare().x] == ' ')
                    System.out.println(Arrays.deepToString(position));
                    //System.out.println("EXRRR " + position[4][move.getEndSquare().x] + " " + position[move.getEndSquare().y][move.getEndSquare().x]);
                
                position[4][move.getEndSquare().x] = ' ';
            }

            position[8][0] = ' ';
        }
        else
        {
            if ((startPiece == 'p' || startPiece == 'P') && Math.abs(move.getStartSquare().y-move.getEndSquare().y) > 1)
            {
                position[8][0] = (char)(move.getEndSquare().x+48);

            }
            else
            {
                position[8][0] = ' ';
            }
        }
        
        move.setCapturedPiece(position[move.getEndSquare().y][move.getEndSquare().x]);
        position[move.getEndSquare().y][move.getEndSquare().x] = startPiece;
        position[move.getStartSquare().y][move.getStartSquare().x] = ' ';
        
        if (startPiece == 'p' || startPiece == 'P')
        {
            if (Character.isLowerCase(startPiece))
            {
                if (move.getEndSquare().y == 0)
                {
                    position[move.getEndSquare().y][move.getEndSquare().x] = Character.toLowerCase(nextPromotion);
                    move.setPromotion(true);
                }
            }
            else
            {
                if (move.getEndSquare().y == 7)
                {
                    position[move.getEndSquare().y][move.getEndSquare().x] = move.getPromotingTo();//Character.toUpperCase(nextPromotion);
                    move.setPromotion(true);
                }
            }
        }
        
        
        
        if (castle == 1)
        {
            char rook = position[move.getStartSquare().y][7];
            position[move.getStartSquare().y][7] = ' ';
            position[move.getStartSquare().y][5] = rook;
        }
        else if (castle == -1)
        {
            char rook = position[move.getStartSquare().y][0];
            position[move.getStartSquare().y][0] = ' ';
            position[move.getStartSquare().y][3] = rook;
        }
        
        
        if (actual) //If the move is actually being made on the real game board as opposed to moves made by the AI in the minmax algorithm
        {
            //Used for checking for 3-fold repetition
            
            /*
            reachedPositions[reachedPositionsIndex] = calculateZobrist(position);
            reachedPositionsIndex++;
            if (reachedPositionsIndex >= reachedPositions.length)
            {
                reachedPositionsIndex = 0;
            }
            */
            reachedPositions.add(zobrist);
            
            movedFrom = move.getStartSquare();
            movedTo = move.getEndSquare();
            
            isGameOver = checkGameOver(position, !Character.isLowerCase(startPiece));
            
            if (isGameOver)
            {
                clock.stop();
                String message = "";
                switch (checkWinner(position))
                {
                    case 0:
                        message = "The game is drawn.";
                        break;
                    case 1:
                        message = "Black wins.";
                        break;
                    case 2:
                        message = "White wins.";
                        break;
                }
                System.out.println("Game over. " + message);
                
                System.out.println(moveLog);
                
                if (checkWinner(position) == 2)
                {
                    playerOneWon = true;
                    playerTwoWon = false;
                }
                else if (checkWinner(position) == 1)
                {
                    playerOneWon = false;
                    playerTwoWon = true;
                }
                else
                {
                    playerOneWon = false;
                    playerTwoWon = false;
                }
            }
        }
        
        return zobrist;
    }
    
    public static void setNextPromotion(char toPromote)
    {
        nextPromotion = toPromote;
    }
    
    //Returns true if the given position is of a completed game, false otherwise
    public static boolean checkGameOver(char[][] position, boolean isWhite)
    {
        boolean gameOver = false;
        
        LinkedList<Move> moves = getLegalMoves(position, isWhite, false);
        
        if (moves.isEmpty())
            gameOver = true;
        
        return gameOver;
    }
    
    public static long getZobrist()
    {
        return currentZobrist;
    }
    
    public static HashSet<Long> getPreviousPositions()
    {
        return reachedPositions;
    }
    
    public static int checkWinner(char[][] position) //Returns 2 if white won, 1 if black wone, 0 if draw
    {
        int result = 0;
                
        for (int i = 0; i < 8; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                if (position[i][j] == 'k')
                {
                    if (isAttacked(position, false, j, i))
                    {
                        result = 1;
                    }
                }
                if (position[i][j] == 'K')
                {
                    if (isAttacked(position, true, j, i))
                    {
                        result = 2;
                    }
                }
            }
        }
        
        return result;
    }
    
    //Finds all possible pawn moves for a given square and color
    public static void findPawnMoves(char[][] position, boolean isWhite, int x, int y, LinkedList legalMoves)
    {
        if (isWhite && Character.isUpperCase(position[y][x])) //if it's white's move and the piece is black, return
            return;
        if (!isWhite && Character.isLowerCase(position[y][x])) //And vice versa
            return;
        
        if (isWhite)
        {
            if (y == 3 && position[8][0] != ' ')
            {
                if (x-1 == Character.getNumericValue(position[8][0]) || x+1 == Character.getNumericValue(position[8][0]))
                {
                    addMove(new Move(new Point(x, y), new Point(Character.getNumericValue(position[8][0]), y-1)), legalMoves, position);
                }
            }
                
            if (y > 0)
                {
                    if (position[y-1][x] == ' ')
                    {
                        addMove(new Move(new Point(x, y), new Point(x, y-1)), legalMoves, position);
                        if (y == 6 && position[y-2][x] == ' ')
                            addMove(new Move(new Point(x, y), new Point(x, y-2)), legalMoves, position);
                    }

                    if (x > 0)
                    {
                        if (Character.isUpperCase(position[y-1][x-1]))
                        {
                            addMove(new Move(new Point(x, y), new Point(x-1, y-1)), legalMoves, position);
                        }
                    }
                    if (x < 7)
                    {
                        if (Character.isUpperCase(position[y-1][x+1]))
                        {
                            addMove(new Move(new Point(x, y), new Point(x+1, y-1)), legalMoves, position);
                        }
                    }
                }
        }
        else
        {
            if (y == 4 && position[8][0] != ' ')
            {
                if (x-1 == Character.getNumericValue(position[8][0]) || x+1 == Character.getNumericValue(position[8][0]))
                    addMove(new Move(new Point(x, y), new Point(Character.getNumericValue(position[8][0]), y+1)), legalMoves, position);
            }
            
            if (y < 7)
            {
                if (position[y+1][x] == ' ')
                {
                    addMove(new Move(new Point(x, y), new Point(x, y+1)), legalMoves, position);
                    if (y == 1 && position[y+2][x] == ' ')
                        addMove(new Move(new Point(x, y), new Point(x, y+2)), legalMoves, position);
                }


                if (x > 0)
                {
                    if (Character.isLowerCase(position[y+1][x-1]))
                    {
                        addMove(new Move(new Point(x, y), new Point(x-1, y+1)), legalMoves, position);
                    }
                }
                if (x < 7)
                {
                    if (Character.isLowerCase(position[y+1][x+1]))
                    {
                        addMove(new Move(new Point(x, y), new Point(x+1, y+1)), legalMoves, position);
                    }
                }
            }
        }
    }
    
    public static int findKnightMoves(char[][] position, boolean isWhite, int x, int y)
    {
        int moves = 0;
        
        if (x > 1)
        {
            if (y > 0)
            {
                if (position[y-1][x-2] == ' ' || Character.isUpperCase(position[y-1][x-2])^(!isWhite))
                    moves++;
            }
            if (y < 7)
            {
                if (position[y+1][x-2] == ' ' ||Character.isUpperCase(position[y+1][x-2])^(!isWhite))
                    moves++;
            }
        }

        if (x < 6)
        {
            if (y > 0)
            {
                if (position[y-1][x+2] == ' ' || Character.isUpperCase(position[y-1][x+2])^(!isWhite))
                    moves++;
            }
            if (y < 7)
            {
                if (position[y+1][x+2] == ' ' || Character.isUpperCase(position[y+1][x+2])^(!isWhite))
                    moves++;
            }
        }

        if (y > 1)
        {
            if (x > 0)
            {
                if (position[y-2][x-1] == ' ' || Character.isUpperCase(position[y-2][x-1])^(!isWhite))
                    moves++;
            }
            if (x < 7)
            {
                if (position[y-2][x+1] == ' ' ||Character.isUpperCase(position[y-2][x+1])^(!isWhite))
                    moves++;
            }
        }

        if (y < 6)
        {
            if (x > 0)
            {
                if (position[y+2][x-1] == ' ' || Character.isUpperCase(position[y+2][x-1])^(!isWhite))
                    moves++;
            }
            if (x < 7)
            {
                if (position[y+2][x+1] == ' ' ||Character.isUpperCase(position[y+2][x+1])^(!isWhite))
                    moves++;
            }
        }
        
        return moves;
    }
    
    public static void findKnightMoves(char[][] position, boolean isWhite, int x, int y, LinkedList legalMoves)
    {
        if (isWhite && Character.isUpperCase(position[y][x]))
            return;
        if (!isWhite && Character.isLowerCase(position[y][x]))
            return;
            
        if (x > 1)
        {
            if (y > 0)
            {
                if (position[y-1][x-2] == ' ' || Character.isUpperCase(position[y-1][x-2])^(!isWhite))
                    addMove(new Move(new Point(x, y), new Point(x-2, y-1)), legalMoves, position);
            }
            if (y < 7)
            {
                if (position[y+1][x-2] == ' ' ||Character.isUpperCase(position[y+1][x-2])^(!isWhite))
                    addMove(new Move(new Point(x, y), new Point(x-2, y+1)), legalMoves, position);
            }
        }

        if (x < 6)
        {
            if (y > 0)
            {
                if (position[y-1][x+2] == ' ' || Character.isUpperCase(position[y-1][x+2])^(!isWhite))
                    addMove(new Move(new Point(x, y), new Point(x+2, y-1)), legalMoves, position);
            }
            if (y < 7)
            {
                if (position[y+1][x+2] == ' ' || Character.isUpperCase(position[y+1][x+2])^(!isWhite))
                    addMove(new Move(new Point(x, y), new Point(x+2, y+1)), legalMoves, position);
            }
        }

        if (y > 1)
        {
            if (x > 0)
            {
                if (position[y-2][x-1] == ' ' || Character.isUpperCase(position[y-2][x-1])^(!isWhite))
                    addMove(new Move(new Point(x, y), new Point(x-1, y-2)), legalMoves, position);
            }
            if (x < 7)
            {
                if (position[y-2][x+1] == ' ' ||Character.isUpperCase(position[y-2][x+1])^(!isWhite))
                    addMove(new Move(new Point(x, y), new Point(x+1, y-2)), legalMoves, position);
            }
        }

        if (y < 6)
        {
            if (x > 0)
            {
                if (position[y+2][x-1] == ' ' || Character.isUpperCase(position[y+2][x-1])^(!isWhite))
                    addMove(new Move(new Point(x, y), new Point(x-1, y+2)), legalMoves, position);
            }
            if (x < 7)
            {
                if (position[y+2][x+1] == ' ' ||Character.isUpperCase(position[y+2][x+1])^(!isWhite))
                    addMove(new Move(new Point(x, y), new Point(x+1, y+2)), legalMoves, position);
            }
        }
    }
    
    public static int findBishopMoves(char[][] position, boolean isWhite, int x, int y)
    {
        int moves = 0;
        
        int currentX = x;
        int currentY = y;
        
        while (currentX > 0 && currentY > 0)
        {
            currentX -= 1;
            currentY -= 1;
            if (position[currentY][currentX] == ' ')
            {
                moves++;
            }
            else if (Character.isUpperCase(position[currentY][currentX])^(!isWhite))
            {
                moves++;
                break;
            }
            else
                break;
        }
        
        currentX = x;
        currentY = y;
        
        while (currentX > 0 && currentY < 7)
        {
            currentX -= 1;
            currentY += 1;
            if (position[currentY][currentX] == ' ')
            {
                moves++;
            }
            else if (Character.isUpperCase(position[currentY][currentX])^(!isWhite))
            {
                moves++;
                break;
            }
            else
                break;
        }
        
        currentX = x;
        currentY = y;
        
        while (currentX < 7 && currentY > 0)
        {
            currentX += 1;
            currentY -= 1;
            if (position[currentY][currentX] == ' ')
            {
                moves++;
            }
            else if (Character.isUpperCase(position[currentY][currentX])^(!isWhite))
            {
                moves++;
                break;
            }
            else
                break;
        }
        
        currentX = x;
        currentY = y;
        
        while (currentX < 7 && currentY < 7)
        {
            currentX += 1;
            currentY += 1;
            if (position[currentY][currentX] == ' ')
            {
                moves++;
            }
            else if (Character.isUpperCase(position[currentY][currentX])^(!isWhite))
            {
                moves++;
                break;
            }
            else
                break;
        }
        
        return moves;
    }
    
    public static void findBishopMoves(char[][] position, boolean isWhite, int x, int y, LinkedList legalMoves)
    {
        if (isWhite && Character.isUpperCase(position[y][x]))
            return;
        if (!isWhite && Character.isLowerCase(position[y][x]))
            return;
        
        int currentX = x;
        int currentY = y;
        
        while (currentX > 0 && currentY > 0)
        {
            currentX -= 1;
            currentY -= 1;
            if (position[currentY][currentX] == ' ')
            {
                addMove(new Move(new Point(x, y), new Point(currentX, currentY)), legalMoves, position);
            }
            else if (Character.isUpperCase(position[currentY][currentX])^(!isWhite))
            {
                addMove(new Move(new Point(x, y), new Point(currentX, currentY)), legalMoves, position);
                break;
            }
            else
                break;
        }
        
        currentX = x;
        currentY = y;
        
        while (currentX > 0 && currentY < 7)
        {
            currentX -= 1;
            currentY += 1;
            if (position[currentY][currentX] == ' ')
            {
                addMove(new Move(new Point(x, y), new Point(currentX, currentY)), legalMoves, position);
            }
            else if (Character.isUpperCase(position[currentY][currentX])^(!isWhite))
            {
                addMove(new Move(new Point(x, y), new Point(currentX, currentY)), legalMoves, position);
                break;
            }
            else
                break;
        }
        
        currentX = x;
        currentY = y;
        
        while (currentX < 7 && currentY > 0)
        {
            currentX += 1;
            currentY -= 1;
            if (position[currentY][currentX] == ' ')
            {
                addMove(new Move(new Point(x, y), new Point(currentX, currentY)), legalMoves, position);
            }
            else if (Character.isUpperCase(position[currentY][currentX])^(!isWhite))
            {
                addMove(new Move(new Point(x, y), new Point(currentX, currentY)), legalMoves, position);
                break;
            }
            else
                break;
        }
        
        currentX = x;
        currentY = y;
        
        while (currentX < 7 && currentY < 7)
        {
            currentX += 1;
            currentY += 1;
            if (position[currentY][currentX] == ' ')
            {
                addMove(new Move(new Point(x, y), new Point(currentX, currentY)), legalMoves, position);
            }
            else if (Character.isUpperCase(position[currentY][currentX])^(!isWhite))
            {
                addMove(new Move(new Point(x, y), new Point(currentX, currentY)), legalMoves, position);
                break;
            }
            else
                break;
        }
    }
    
    public static int findRookMoves(char[][] position, boolean isWhite, int x, int y)
    {
        int moves = 0;
        
        int currentX = x;
        int currentY = y;
        
        while (currentX > 0)
        {
            currentX -= 1;
            if (position[currentY][currentX] == ' ')
                moves++;
            else if (Character.isUpperCase(position[currentY][currentX])^(!isWhite))
            {
                moves++;
                break;
            }
            else
                break;
        }
        
        currentX  = x;
        
        while (currentX < 7)
        {
            currentX += 1;
            if (position[currentY][currentX] == ' ')
                moves++;
            else if (Character.isUpperCase(position[currentY][currentX])^(!isWhite))
            {
                moves++;
                break;
            }
            else
                break;
        }
        
        currentX = x;
        
        while (currentY > 0)
        {
            currentY -= 1;
            if (position[currentY][currentX] == ' ')
                moves++;
            else if (Character.isUpperCase(position[currentY][currentX])^(!isWhite))
            {
                moves++;
                break;
            }
            else
                break;
        }
        
        currentY = y;
        
        while (currentY < 7)
        {
            currentY += 1;
            if (position[currentY][currentX] == ' ')
                moves++;
            else if (Character.isUpperCase(position[currentY][currentX])^(!isWhite))
            {
                moves++;
                break;
            }
            else
                break;
        }
        
        return moves;
    }
    
    public static void findRookMoves(char[][] position, boolean isWhite, int x, int y, LinkedList legalMoves)
    {
        if (isWhite && Character.isUpperCase(position[y][x]))
            return;
        if (!isWhite && Character.isLowerCase(position[y][x]))
            return;
        
        int currentX = x;
        int currentY = y;
        
        while (currentX > 0)
        {
            currentX -= 1;
            if (position[currentY][currentX] == ' ')
                addMove(new Move(new Point(x, y), new Point(currentX, currentY)), legalMoves, position);
            else if (Character.isUpperCase(position[currentY][currentX])^(!isWhite))
            {
                addMove(new Move(new Point(x, y), new Point(currentX, currentY)), legalMoves, position);
                break;
            }
            else
                break;
        }
        
        currentX  = x;
        
        while (currentX < 7)
        {
            currentX += 1;
            if (position[currentY][currentX] == ' ')
                addMove(new Move(new Point(x, y), new Point(currentX, currentY)), legalMoves, position);
            else if (Character.isUpperCase(position[currentY][currentX])^(!isWhite))
            {
                addMove(new Move(new Point(x, y), new Point(currentX, currentY)), legalMoves, position);
                break;
            }
            else
                break;
        }
        
        currentX = x;
        
        while (currentY > 0)
        {
            currentY -= 1;
            if (position[currentY][currentX] == ' ')
                addMove(new Move(new Point(x, y), new Point(currentX, currentY)), legalMoves, position);
            else if (Character.isUpperCase(position[currentY][currentX])^(!isWhite))
            {
                addMove(new Move(new Point(x, y), new Point(currentX, currentY)), legalMoves, position);
                break;
            }
            else
                break;
        }
        
        currentY = y;
        
        while (currentY < 7)
        {
            currentY += 1;
            if (position[currentY][currentX] == ' ')
                addMove(new Move(new Point(x, y), new Point(currentX, currentY)), legalMoves, position);
            else if (Character.isUpperCase(position[currentY][currentX])^(!isWhite))
            {
                addMove(new Move(new Point(x, y), new Point(currentX, currentY)), legalMoves, position);
                break;
            }
            else
                break;
        }
    }
    
    //Determines if a given square can be attacked by a given color
    public static boolean isAttacked(char[][] position, boolean whiteAttacker, int x, int y)
    {
        boolean isAttacked = false;
        
        int currentX = x;
        int currentY = y;
        
        //Check pawns
        if (whiteAttacker)
        {
            if (y < 7)
            {
                if (x < 7)
                 {
                     if (position[y+1][x+1] == 'p')
                         isAttacked = true;
                 }
                if (x > 0)
                {
                    if (position[y+1][x-1] == 'p')
                        isAttacked = true;
                }
            }
        }
        else
        {
            if (y > 0)
            {
                if (x < 7)
                 {
                     if (position[y-1][x+1] == 'P')
                         isAttacked = true;
                 }
                if (x > 0)
                {
                    if (position[y-1][x-1] == 'P')
                        isAttacked = true;
                }
            }
        }
        
        if (isAttacked)
            return true;
        
        //Check bishops/queens
        
        while (currentX > 0 && currentY > 0)
        {
            currentX--;
            currentY--;
            
            if (((position[currentY][currentX] == 'b' || position[currentY][currentX] == 'B') || (position[currentY][currentX] == 'q' || position[currentY][currentX] == 'Q')) && Character.isUpperCase(position[currentY][currentX])^(whiteAttacker))
            {
                isAttacked = true;
                break;
            }
            else if (position[currentY][currentX] != ' ')
                break;
        }
        
        currentX = x;
        currentY = y;
        
        while (currentX > 0 && currentY < 7)
        {
            currentX--;
            currentY++;
            
            if (((position[currentY][currentX] == 'b' || position[currentY][currentX] == 'B') || (position[currentY][currentX] == 'q' || position[currentY][currentX] == 'Q')) && Character.isUpperCase(position[currentY][currentX])^(whiteAttacker))
            {
                isAttacked = true;
                break;
            }
            else if (position[currentY][currentX] != ' ')
                break;
        }
        
        currentX = x;
        currentY = y;
        
        while (currentX < 7 && currentY > 0)
        {
            currentX++;
            currentY--;
            
            if (((position[currentY][currentX] == 'b' || position[currentY][currentX] == 'B') || (position[currentY][currentX] == 'q' || position[currentY][currentX] == 'Q')) && Character.isUpperCase(position[currentY][currentX])^(whiteAttacker))
            {
                isAttacked = true;
                break;
            }
            else if (position[currentY][currentX] != ' ')
                break;
        }
        
        currentX = x;
        currentY = y;
        
        while (currentX < 7 && currentY < 7)
        {
            currentX++;
            currentY++;
            
            if (((position[currentY][currentX] == 'b' || position[currentY][currentX] == 'B') || (position[currentY][currentX] == 'q' || position[currentY][currentX] == 'Q')) && Character.isUpperCase(position[currentY][currentX])^(whiteAttacker))
            {
                isAttacked = true;
                break;
            }
            else if (position[currentY][currentX] != ' ')
                break;
        }
        
        currentX = x;
        currentY = y;
        
        if (isAttacked)
            return true;
        
        //Check rooks/queens
                
        while (currentX > 0)
        {
            currentX--;
            
            if (((position[currentY][currentX] == 'r' || position[currentY][currentX] == 'R') || (position[currentY][currentX] == 'q' || position[currentY][currentX] == 'Q')) && Character.isUpperCase(position[currentY][currentX])^(whiteAttacker))
            {
                isAttacked = true;
                break;
            }
            else if (position[currentY][currentX] != ' ')
                break;
        }
        
        currentX = x;
        currentY = y;
        
        while (currentX < 7)
        {
            currentX++;
            
            if (((position[currentY][currentX] == 'r' || position[currentY][currentX] == 'R') || (position[currentY][currentX] == 'q' || position[currentY][currentX] == 'Q')) && Character.isUpperCase(position[currentY][currentX])^(whiteAttacker))
            {
                isAttacked = true;
                break;
            }
            else if (position[currentY][currentX] != ' ')
                break;
        }
        
        currentX = x;
        currentY = y;
        
        while (currentY > 0)
        {
            currentY--;
            
            if (((position[currentY][currentX] == 'r' || position[currentY][currentX] == 'R') || (position[currentY][currentX] == 'q' || position[currentY][currentX] == 'Q')) && Character.isUpperCase(position[currentY][currentX])^(whiteAttacker))
            {
                isAttacked = true;
                break;
            }
            else if (position[currentY][currentX] != ' ')
                break;
        }
        
        currentX = x;
        currentY = y;
        
        while (currentY < 7)
        {
            currentY++;
            
            if (((position[currentY][currentX] == 'r' || position[currentY][currentX] == 'R') || (position[currentY][currentX] == 'q' || position[currentY][currentX] == 'Q')) && Character.isUpperCase(position[currentY][currentX])^(whiteAttacker))
            {
                isAttacked = true;
                break;
            }
            else if (position[currentY][currentX] != ' ')
                break;
        }
        
        if (isAttacked)
            return true;
        
        //Check knights
        if (x > 1)
        {
            if (y > 0)
            {
                if ((position[y-1][x-2] == 'n' || position[y-1][x-2] == 'N') && Character.isUpperCase(position[y-1][x-2])^(whiteAttacker))
                    isAttacked = true;
            }
            if (y < 7)
            {
                if ((position[y+1][x-2] == 'n' || position[y+1][x-2] == 'N') && Character.isUpperCase(position[y+1][x-2])^(whiteAttacker))
                    isAttacked = true;
            }
        }

        if (x < 6)
        {
            if (y > 0)
            {
                if ((position[y-1][x+2] == 'n' || position[y-1][x+2] == 'N') && Character.isUpperCase(position[y-1][x+2])^(whiteAttacker))
                    isAttacked = true;
            }
            if (y < 7)
            {
                if ((position[y+1][x+2] == 'n' || position[y+1][x+2] == 'N') && Character.isUpperCase(position[y+1][x+2])^(whiteAttacker))
                    isAttacked = true;
            }
        }

        if (y > 1)
        {
            if (x > 0)
            {
                if ((position[y-2][x-1] == 'n' || position[y-2][x-1] == 'N') && Character.isUpperCase(position[y-2][x-1])^(whiteAttacker))
                    isAttacked = true;
            }
            if (x < 7)
            {
                if ((position[y-2][x+1] == 'n' || position[y-2][x+1] == 'N') && Character.isUpperCase(position[y-2][x+1])^(whiteAttacker))
                    isAttacked = true;
            }
        }

        if (y < 6)
        {
            if (x > 0)
            {
                if ((position[y+2][x-1] == 'n' || position[y+2][x-1] == 'N') && Character.isUpperCase(position[y+2][x-1])^(whiteAttacker))
                    isAttacked = true;
            }
            if (x < 7)
            {
                if ((position[y+2][x+1] == 'n' || position[y+2][x+1] == 'N') && Character.isUpperCase(position[y+2][x+1])^(whiteAttacker))
                    isAttacked = true;
            }
        }
        
        if (isAttacked)
            return true;
        
        
        //Check king moves
        if (x > 0)
        {
            if ((position[y][x-1] == 'k' || position[y][x-1] == 'K') && Character.isUpperCase(position[y][x-1])^(whiteAttacker))
                isAttacked = true;
            if (y > 0)
            {
                if ((position[y-1][x-1] == 'k' || position[y-1][x-1] == 'K') && Character.isUpperCase(position[y-1][x-1])^(whiteAttacker))
                    isAttacked = true;
            }
            if (y < 7)
            {
                if ((position[y+1][x-1] == 'k' || position[y+1][x-1] == 'K') && Character.isUpperCase(position[y+1][x-1])^(whiteAttacker))
                    isAttacked = true;
            }
        }
        
        if (x < 7)
        {
            if ((position[y][x+1] == 'k' || position[y][x+1] == 'K') && Character.isUpperCase(position[y][x+1])^(whiteAttacker))
                isAttacked = true;
            if (y > 0)
            {
                if ((position[y-1][x+1] == 'k' || position[y-1][x+1] == 'K') && Character.isUpperCase(position[y-1][x+1])^(whiteAttacker))
                    isAttacked = true;
            }
            if (y < 7)
            {
                if ((position[y+1][x+1] == 'k' || position[y+1][x+1] == 'K') && Character.isUpperCase(position[y+1][x+1])^(whiteAttacker))
                    isAttacked = true;
            }
        }
        
        if (y > 0)
        {
            if ((position[y-1][x] == 'k' || position[y-1][x] == 'K') && Character.isUpperCase(position[y-1][x])^(whiteAttacker))
                isAttacked = true;
        }
        
        if (y < 7)
        {
            if ((position[y+1][x] == 'k' || position[y+1][x] == 'K') && Character.isUpperCase(position[y+1][x])^(whiteAttacker))
                isAttacked = true;
        }
        
        return isAttacked;
    }
   
    //Add a move to a list of moves and, if applicable, duplicate and add moves for different prompotion options
    private static void addMove(Move move, LinkedList moveList, char[][] position)
    {
        if (position[move.getEndSquare().y][move.getEndSquare().x] != ' ')
        {
            move.setMovingPiece(position[move.getStartSquare().y][move.getStartSquare().x]);
            move.setCapturedPiece(position[move.getEndSquare().y][move.getEndSquare().x]);
            
            if (position[move.getStartSquare().y][move.getStartSquare().x] == 'P' && move.getEndSquare().y == 7)
            {
                Move temp = move.copy();
                temp.setPromotingTo('N');
                moveList.add(0, temp);
                temp = move.copy();
                temp.setPromotingTo('B');
                moveList.add(0, temp);
                temp = move.copy();
                temp.setPromotingTo('R');
                moveList.add(0, temp);
                temp = move.copy();
                temp.setPromotingTo('Q');
                moveList.add(temp);
            }
            else
                moveList.add(0, move);
                //moveList.add(move);
        }
        else
        {
            if (position[move.getStartSquare().y][move.getStartSquare().x] == 'P' && move.getEndSquare().y == 7)
            {
                Move temp = move.copy();
                temp.setPromotingTo('N');
                moveList.add(temp);
                temp = move.copy();
                temp.setPromotingTo('B');
                moveList.add(temp);
                temp = move.copy();
                temp.setPromotingTo('R');
                moveList.add(temp);
                temp = move.copy();
                temp.setPromotingTo('Q');
                moveList.add(temp);
            }
            else
                moveList.add(move);
            //moveList.add(move);
        }
       //moveList.add(move);
    }
    
    public static void findKingMoves(char[][] position, boolean isWhite, int x, int y, LinkedList legalMoves)
    {
        if (isWhite && Character.isUpperCase(position[y][x]))
            return;
        if (!isWhite && Character.isLowerCase(position[y][x]))
            return;
        
        if (isWhite)
        {
            if (position[8][3] == '0' || position[8][3] == '2')
            {
                if (position[8][4] == '0' || position[8][4] == '2')
                {
                    if (position[y][x-1] == ' ' && position[y][x-2] == ' ' && position[y][x-3] == ' ' && !isAttacked(position, false, x, y) && !isAttacked(position, false, x-1, y) && !isAttacked(position, false, x-2, y))
                        addMove(new Move(new Point(x, y), new Point(x-2, y)), legalMoves, position);
                }
                if (position[8][5] == '0' || position[8][5] == '2')
                {
                    if (position[y][x+1] == ' ' && position[y][x+2] == ' ' && !isAttacked(position, false, x, y) && !isAttacked(position, false, x+1, y) && !isAttacked(position, false, x+2, y))
                        addMove(new Move(new Point(x, y), new Point(x+2, y)), legalMoves, position);
                }
            }
        }
        else
        {
            if (position[8][3] == '0' || position[8][3] == '1')
            {
                if (position[8][4] == '0' || position[8][4] == '1')
                {
                    if (position[y][x-1] == ' ' && position[y][x-2] == ' ' && position[y][x-3] == ' ' && !isAttacked(position, true, x, y) && !isAttacked(position, true, x-1, y) && !isAttacked(position, true, x-2, y))
                        addMove(new Move(new Point(x, y), new Point(x-2, y)), legalMoves, position);
                }
                if (position[8][5] == '0' || position[8][5] == '1')
                {
                    if (position[y][x+1] == ' ' && position[y][x+2] == ' ' && !isAttacked(position, true, x, y) && !isAttacked(position, true, x+1, y) && !isAttacked(position, true, x+2, y))
                        addMove(new Move(new Point(x, y), new Point(x+2, y)), legalMoves, position);
                }
            }
        }
        
        if (x > 0)
        {
            if (position[y][x-1] == ' '|| Character.isUpperCase(position[y][x-1])^(!isWhite))
                addMove(new Move(new Point(x, y), new Point(x-1, y)), legalMoves, position);
            if (y > 0)
            {
                if (position[y-1][x-1] == ' '|| Character.isUpperCase(position[y-1][x-1])^(!isWhite))
                    addMove(new Move(new Point(x, y), new Point(x-1, y-1)), legalMoves, position);
            }
            if (y < 7)
            {
                if (position[y+1][x-1] == ' '|| Character.isUpperCase(position[y+1][x-1])^(!isWhite))
                    addMove(new Move(new Point(x, y), new Point(x-1, y+1)), legalMoves, position);
            }
        }
        
        if (x < 7)
        {
            if (position[y][x+1] == ' '|| Character.isUpperCase(position[y][x+1])^(!isWhite))
                addMove(new Move(new Point(x, y), new Point(x+1, y)), legalMoves, position);
            if (y > 0)
            {
                if (position[y-1][x+1] == ' '|| Character.isUpperCase(position[y-1][x+1])^(!isWhite))
                    addMove(new Move(new Point(x, y), new Point(x+1, y-1)), legalMoves, position);
            }
            if (y < 7)
            {
                if (position[y+1][x+1] == ' '|| Character.isUpperCase(position[y+1][x+1])^(!isWhite))
                    addMove(new Move(new Point(x, y), new Point(x+1, y+1)), legalMoves, position);
            }
        }
        
        if (y > 0)
        {
            if (position[y-1][x] == ' '|| Character.isUpperCase(position[y-1][x])^(!isWhite))
                addMove(new Move(new Point(x, y), new Point(x, y-1)), legalMoves, position);
        }
        
        if (y < 7)
        {
            if (position[y+1][x] == ' '|| Character.isUpperCase(position[y+1][x])^(!isWhite))
                addMove(new Move(new Point(x, y), new Point(x, y+1)), legalMoves, position);
        }
    }
    
    public static boolean isInCheck(char[][] position)
    {
        for (int i = 0; i < 8; i++)
        {
            for (int j = 0; j < 8; j++)
            {
                if (position[i][j] == 'k')
                {
                    if (isAttacked(position, false, j, i))
                        return true;
                }
                else if (position[i][j] == 'K')
                {
                    if (isAttacked(position, true, j, i))
                        return true;
                }
            }
        }
        
        return false;
    }
    
    //Finds all capture moves from a list of moves
    public static LinkedList<Move> getCaptures(char[][] position, boolean isWhite)
    {
        LinkedList<Move> captures = getLegalMoves(position, isWhite, false);
        
        
        Move move;
        
        Iterator iter = captures.iterator();
        
        while (iter.hasNext())
        {
            move = (Move)iter.next();
            
            if (! (move.isEnPassant() || position[move.getEndSquare().y][move.getEndSquare().x] != ' ') )
            {
                iter.remove();
            }
        }
        
                
        return captures;
    }
    
    
    //Gets all legal moves from a position.
    public static LinkedList<Move> getLegalMoves(char[][] position, boolean isWhite, boolean sort)
    {
        LinkedList<Move> legalMoves = new LinkedList<>();
        
        //Iterate through board
        for (int y = 0; y < 8; y++)
        {
            for (int x = 0; x < 8; x++)
            {
                switch (Character.toLowerCase(position[y][x])) //Find moves pertaining to the type of piece
                {
                    case ' ':
                        break;
                    case 'p':
                        findPawnMoves(position, isWhite, x, y, legalMoves);
                        break;
                    case 'n':
                        findKnightMoves(position, isWhite, x, y, legalMoves);
                        break;
                    case 'b':
                        findBishopMoves(position, isWhite, x, y, legalMoves);
                        break;
                    case 'r':
                        findRookMoves(position, isWhite, x, y, legalMoves);
                        break;
                    case 'q':
                        findBishopMoves(position, isWhite, x, y, legalMoves); //Queen's moves consist of bishop's moves + rook's moves
                        findRookMoves(position, isWhite, x, y, legalMoves);
                        break;
                    case 'k':
                        findKingMoves(position, isWhite, x, y, legalMoves);
                        break;
                }
            }
        }
        
        
        LinkedList<Move> newMoves = new LinkedList<>(); //Intended to be used for sorting moves by probable value, not currently implemented
               
        
        //We must now remove all moves which are illegal because of circumstances not detected in the simple move finding methods
        Iterator iter = legalMoves.iterator();

        
        Move move;
        Point kingPosition;
        char destKing;
        int index;
        
        //char[][] oldPosition = cloneBoard(position);
        //sort = false;
        while (iter.hasNext())
        {         
            move = (Move)iter.next();
            kingPosition = null;
            destKing = isWhite ? 'k' : 'K';
            
            //position = Board.cloneBoard(position);
            if (!move.isEnPassant())
                move.setCapturedPiece(position[move.getEndSquare().y][move.getEndSquare().x]);
            
            makeMove(position, move, false, 0);
            
            //Find king position            
            outerloop:
            for (int i = 0; i < 8; i++)
            {
                for (int j = 0; j < 8; j++)
                {
                    if (position[i][j] == destKing)
                    {
                        kingPosition = new Point(j, i);
                        break outerloop;
                    }
                }
            }
            
            
            //If the king is attacked, the move cannot be legal
            if (isAttacked(position, !isWhite, kingPosition.x, kingPosition.y))
            {
                iter.remove();
            }
            else if (sort) //Not currently used
            {
                if (move.getCapturedPiece() != ' ')
                {
                    index = 0;
                    //System.out.println("Tet");
                }
                else
                    index = newMoves.size();
                newMoves.add(index, move);
            }
            
            unmakeMove(position, move, 0);
        }

        
        /*
        if (!Arrays.deepEquals(oldPosition, position))
        {
            System.out.println("Mismatch");
            if (oldPosition[8][0] != position[8][0])
                System.out.println("Error relates to en passant value");
            
            System.out.println(Arrays.deepToString(oldPosition));
            System.out.println(Arrays.deepToString(position));
            System.out.println(oldPosition[8].length + " " + position[8].length);
            System.out.println("\n\n");
        }*/
        
        if (sort)
            return newMoves;
        return legalMoves;
    }
    
    public static Point getSelected()
    {
        return selected;
    }
    
    //Checks if a given set of coordinates is on top of the rendered board
    private static boolean onBoard(Point coords)
    {
        return coords.x > POSITION[0] && coords.y > POSITION[1] && coords.x < POSITION[0]+SQUARE_SIZE*8 && coords.y < POSITION[1]+SQUARE_SIZE*8;
    }
    
    //Converts a mouse position to a set of coordinates of the board.
    public static Point toBoardCoords(Point coords)
    {
        if (coords.x > POSITION[0] && coords.y > POSITION[1] && coords.x < POSITION[0]+SQUARE_SIZE*8 && coords.y < POSITION[1]+SQUARE_SIZE*8)
        {
            int x, y;

            x = (coords.x-POSITION[0])/SQUARE_SIZE;
            y = (coords.y-POSITION[1])/SQUARE_SIZE;
            
            return new Point(x, y);
        }
        return null;
    }
    
    public static void checkPromotionSelection(Point coords)
    {
        int offset = 20;
        if (coords.x > PROMOTION_PANEL_POSITION[0] && coords.x < PROMOTION_PANEL_POSITION[0]+SQUARE_SIZE && coords.y > PROMOTION_PANEL_POSITION[1] && coords.y < PROMOTION_PANEL_POSITION[1] + 500)
        {
            if (coords.y > PROMOTION_PANEL_POSITION[1] + 500 - SQUARE_SIZE-offset)
            {
                nextPromotion = 'q';
            }
            
            else if (coords.y > PROMOTION_PANEL_POSITION[1] + 500 - SQUARE_SIZE*2-offset)
            {
                nextPromotion = 'r';
            }
            
            else if (coords.y > PROMOTION_PANEL_POSITION[1] + 500 - SQUARE_SIZE*3-offset)
            {
                nextPromotion = 'b';
            }
            
            else if (coords.y > PROMOTION_PANEL_POSITION[1] + 500 - SQUARE_SIZE*4-offset)
            {
                nextPromotion = 'n';
            }
        }
    }
    
    public static Point fromBoardCoords(Point coords)
    {
        return new Point(coords.x*SQUARE_SIZE+POSITION[0]+SQUARE_SIZE/2, coords.y*SQUARE_SIZE+POSITION[1]+SQUARE_SIZE/2);
    }
    
    public static void deSelect()
    {
        selected.setLocation(-1, -1);
    }
    
    public static boolean hasSelected()
    {
        return !(selected.x == -1);
    }
    
    public static String getMoves()
    {
        return moveLog;
    }
}
