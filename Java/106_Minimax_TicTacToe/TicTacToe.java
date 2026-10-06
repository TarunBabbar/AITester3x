import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 106 - Tic-tac-toe with minimax and alpha-beta pruning: the engine searches
 * every continuation, so it can never lose. Pruning cuts the tree without
 * changing the answer, which is checked by comparing both against each other.
 *
 * The engine plays 'O'; the opponent plays 'X'.
 *
 * Compile and run:
 *   javac TicTacToe.java
 *   java TicTacToe
 */
public class TicTacToe {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final char EMPTY = '.';
    static final char HUMAN = 'X';
    static final char ENGINE = 'O';

    static final int[][] LINES = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},   // rows
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},   // columns
            {0, 4, 8}, {2, 4, 6},              // diagonals
    };

    /** 'X' or 'O' for a win, 'D' for a full board, ' ' while undecided. */
    static char winner(char[] board) {
        for (int[] line : LINES) {
            char first = board[line[0]];
            if (first != EMPTY && first == board[line[1]] && first == board[line[2]]) {
                return first;
            }
        }
        for (char square : board) {
            if (square == EMPTY) {
                return ' ';
            }
        }
        return 'D';
    }

    static char[] parse(String layout) {
        char[] board = new char[9];
        String cleaned = layout.replaceAll("[^XO.]", "");
        if (cleaned.length() != 9) {
            throw new IllegalArgumentException("a board needs nine squares, got " + cleaned.length());
        }
        for (int i = 0; i < 9; i++) {
            board[i] = cleaned.charAt(i);
        }
        return board;
    }

    static String render(char[] board) {
        StringBuilder out = new StringBuilder();
        for (int row = 0; row < 3; row++) {
            out.append(board[row * 3]).append(' ').append(board[row * 3 + 1])
                    .append(' ').append(board[row * 3 + 2]);
            if (row < 2) {
                out.append('\n');
            }
        }
        return out.toString();
    }

    static final class Engine {
        private final boolean pruning;
        private long nodes;

        Engine(boolean pruning) {
            this.pruning = pruning;
        }

        long nodes() {
            return nodes;
        }

        /**
         * The best square for the engine, found by trying each one and scoring the
         * position that follows. Ties go to the lowest square, so it is repeatable.
         */
        int bestMove(char[] board) {
            nodes = 0;
            int bestScore = Integer.MIN_VALUE;
            int bestSquare = -1;
            for (int square = 0; square < 9; square++) {
                if (board[square] != EMPTY) {
                    continue;
                }
                board[square] = ENGINE;
                int score = minimax(board, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
                board[square] = EMPTY;
                if (score > bestScore) {
                    bestScore = score;
                    bestSquare = square;
                }
            }
            return bestSquare;
        }

        /** The score of the position with the engine to move. */
        int score(char[] board) {
            nodes = 0;
            return minimax(board, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
        }

        /**
         * Depth is subtracted from a win and added to a loss, so a quicker win and a
         * slower loss are both preferred.
         */
        private int minimax(char[] board, int depth, int alpha, int beta, boolean maximizing) {
            nodes++;
            char result = winner(board);
            if (result == ENGINE) {
                return 10 - depth;
            }
            if (result == HUMAN) {
                return depth - 10;
            }
            if (result == 'D') {
                return 0;
            }

            if (maximizing) {
                int best = Integer.MIN_VALUE;
                for (int square = 0; square < 9; square++) {
                    if (board[square] != EMPTY) {
                        continue;
                    }
                    board[square] = ENGINE;
                    best = Math.max(best, minimax(board, depth + 1, alpha, beta, false));
                    board[square] = EMPTY;
                    if (pruning) {
                        alpha = Math.max(alpha, best);
                        if (beta <= alpha) {
                            break;                  // the minimiser would never allow this
                        }
                    }
                }
                return best;
            }

            int best = Integer.MAX_VALUE;
            for (int square = 0; square < 9; square++) {
                if (board[square] != EMPTY) {
                    continue;
                }
                board[square] = HUMAN;
                best = Math.min(best, minimax(board, depth + 1, alpha, beta, true));
                board[square] = EMPTY;
                if (pruning) {
                    beta = Math.min(beta, best);
                    if (beta <= alpha) {
                        break;                      // the maximiser has a better option already
                    }
                }
            }
            return best;
        }
    }

    record Game(char[] board, char result, int moves) {
    }

    /** Play the engine against a random opponent. */
    static Game playAgainstRandom(long seed) {
        char[] board = parse(".........");
        Engine engine = new Engine(true);
        Random random = new Random(seed);
        int moves = 0;
        while (winner(board) == ' ') {
            if (moves % 2 == 0) {
                int square = engine.bestMove(board);
                board[square] = ENGINE;             // the engine moves first here
            } else {
                List<Integer> free = new ArrayList<>();
                for (int square = 0; square < 9; square++) {
                    if (board[square] == EMPTY) {
                        free.add(square);
                    }
                }
                board[free.get(random.nextInt(free.size()))] = HUMAN;
            }
            moves++;
        }
        return new Game(board, winner(board), moves);
    }

    /** Two perfect players. */
    static Game playPerfectly() {
        char[] board = parse(".........");
        Engine first = new Engine(true);
        Engine second = new Engine(true);
        int moves = 0;
        while (winner(board) == ' ') {
            char player = moves % 2 == 0 ? HUMAN : ENGINE;
            Engine engine = moves % 2 == 0 ? first : second;
            int square = engine.bestMove(board);
            board[square] = player;
            moves++;
        }
        return new Game(board, winner(board), moves);
    }

    public static void main(String[] args) {
        // ---- the rules --------------------------------------------------------
        check(winner(parse("XXX" + "OO." + "...")) == HUMAN, "a row wins");
        check(winner(parse("X..X..X..")) == HUMAN, "a column wins");
        check(winner(parse("X...X...X")) == HUMAN, "the main diagonal wins");
        check(winner(parse("..X.X.X..")) == HUMAN, "the other diagonal wins");
        // A genuinely drawn board: five X and four O, no line anywhere.
        check(winner(parse("XOXOOXXXO")) == 'D', "a full board with no line is a draw");
        check(winner(parse("XO.......")) == ' ', "an unfinished game is undecided");
        check(winner(parse("OOOXX.X.X")) == ENGINE, "the engine's row is found");
        System.out.println("rules        : rows, columns, diagonals, draws and unfinished games");

        Engine engine = new Engine(true);

        // ---- the engine takes a win it can see --------------------------------
        // X has 0 and 1, so square 2 wins for X. The engine's own pair is 3 and 4,
        // so it finishes with square 5 first, because that wins immediately.
        char[] racingToWin = parse("XX.OO....");
        check(engine.bestMove(racingToWin) == 5, "the engine takes its own win rather than blocking");
        System.out.println("immediate win: plays square 5, not the block");

        // ---- and blocks when it cannot win ------------------------------------
        char[] mustBlock = parse("XX.......");
        check(engine.bestMove(mustBlock) == 2, "with nothing better, it blocks the threat");
        System.out.println("block        : plays square 2 to stop X");

        // ---- a corner opening -------------------------------------------------
        char[] empty = parse(".........");
        int opening = engine.bestMove(empty);
        check(opening >= 0 && opening < 9, "the engine opens somewhere legal");
        System.out.println("opening      : square " + opening);

        // ---- the empty board is drawn with perfect play -----------------------
        Engine full = new Engine(false);
        check(engine.score(parse(".........")) == 0, "with perfect play the empty board is a draw");
        check(full.score(parse(".........")) == 0, "and pruning does not change that");
        System.out.println("perfect play : the empty board scores 0, so neither side can force a win");

        // ---- pruning gives the same answer with less work ---------------------
        long prunedNodes = engine.nodes();
        long fullNodes = full.nodes();
        check(prunedNodes < fullNodes,
                "pruning examined fewer nodes: " + prunedNodes + " against " + fullNodes);
        System.out.println("pruning      : " + prunedNodes + " nodes with alpha-beta, "
                + fullNodes + " without (" + (100 * prunedNodes / fullNodes) + "%)");

        // the two agree on several positions, not just the empty board
        String[] positions = {".........", "X........", "XO.......", "X..O.....", "XOXXOO..."};
        for (String position : positions) {
            char[] board = parse(position);
            if (winner(board) != ' ') {
                continue;
            }
            check(new Engine(true).score(board) == new Engine(false).score(board),
                    "both engines agree on " + position);
        }
        System.out.println("agreement    : both engines score every test position the same");

        // ---- never loses against random play ---------------------------------
        int wins = 0;
        int draws = 0;
        int losses = 0;
        for (long seed = 0; seed < 50; seed++) {
            Game game = playAgainstRandom(seed);
            if (game.result() == ENGINE) {
                wins++;
            } else if (game.result() == 'D') {
                draws++;
            } else {
                losses++;
            }
        }
        check(losses == 0, "a perfect engine never loses, but lost " + losses + " times");
        check(wins + draws == 50, "the other fifty games were won or drawn");
        System.out.println("50 games     : " + wins + " won, " + draws + " drawn, " + losses + " lost");

        // ---- perfect against perfect is always a draw -------------------------
        Game perfect = playPerfectly();
        check(perfect.result() == 'D', "two perfect players draw: " + perfect.result());
        check(perfect.moves() == 9, "and the board fills up: " + perfect.moves() + " moves");
        System.out.println("engine vs it :\n" + render(perfect.board()));

        // ---- the engine always returns a legal move ---------------------------
        for (String position : positions) {
            char[] board = parse(position);
            if (winner(board) != ' ') {
                continue;
            }
            int reply = new Engine(true).bestMove(board);
            check(reply >= 0 && board[reply] == EMPTY,
                    "the reply for " + position + " is an empty square");
        }
        System.out.println("legal moves  : every reply lands on an empty square");

        // ---- invalid boards ---------------------------------------------------
        for (String bad : new String[] {"", "X", "..........", "XO"}) {
            try {
                parse(bad);
                throw new AssertionError("should have been rejected: " + bad);
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
        System.out.println("bad boards   : four malformed layouts rejected");

        // ---- a game against a named opening -----------------------------------
        char[] game = parse("...X.....");
        int reply = engine.bestMove(game);
        check(game[reply] == EMPTY, "the reply is on an empty square");
        System.out.println("reply to X centre: square " + reply);
        System.out.println("All checks passed.");
    }
}
