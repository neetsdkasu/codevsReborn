// CODE VS - Reborn
// author: Leonrdone @ NEETSDKASU
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;

interface AI {
    String getName();
    void init(Pack[] packs) throws Exception;
    Command getCommand(Turn turn) throws Exception;
}

public class Main {
    public static void main(String[] args) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintStream out = System.out;

        AI ai = new MyAI();

        out.println(ai.getName());
        out.flush();

        Pack[] packs = new Pack[Turn.MAX_TURN];
        for (int i = 0; i < packs.length; ++i) {
            packs[i] = Pack.scan(in);
        }
        ai.init(packs);

        for (int i = 0; i < 500; ++i) {
            Turn turn = Turn.scan(in);
            Command command = ai.getCommand(turn);
            out.println(command);
            out.flush();
        }
    }
}

interface Command {}

class Turn {

    static Turn scan(BufferedReader in) throws Exception {
        Turn turn = new Turn();
        turn.count = Integer.parseInt(in.readLine());
        turn.states[0] = State.scan(in);
        turn.states[1] = State.scan(in);
        return turn;
    }

    static final int MAX_TURN = 500;

    State[] states = new State[2];
    int count = 0;

    private Turn() {}

    int getCount() {
        return count;
    }

    State getMyState() {
        return states[0];
    }
}

class State {
    static State scan(BufferedReader in) throws Exception {
        State state = new State();
        state.timeLeft = Long.parseLong(in.readLine());
        state.stock = Integer.parseInt(in.readLine());
        state.gauge = Integer.parseInt(in.readLine());
        state.score = Integer.parseInt(in.readLine());
        state.field = Field.scan(in);
        return state;
    }

    static final int[] CHAIN_SCORES = new int[50];
    static {
        double s = 1.0;
        for (int i = 1; i < CHAIN_SCORES.length; ++i) {
            s *= 1.3;
            CHAIN_SCORES[i] = CHAIN_SCORES[i - 1] + (int)Math.floor(s);
        }
    }

    long timeLeft;
    int stock, gauge, score;
    Field field;

    private State() {}

    boolean isGameOver() {
        return field.isDead();
    }

    State getCopy() {
        State state = new State();
        state.timeLeft = timeLeft;
        state.stock = stock;
        state.gauge = gauge;
        state.score = score;
        state.field = field.getCopy();
        return state;
    }

    int putPack(Pack pack) {
        field.putPack(pack);
        int chain = 0;
        for (;;) {
            if (!field.erase()) {
                break;
            }
            ++chain;
            if (!field.drop()) {
                break;
            }
        }
        int add = CHAIN_SCORES[chain];
        stock -= add / 2;
        score += add;
        if (chain > 0) {
            gauge += 8;
        }
        return chain;
    }
}

class Field {

    static Field scan(BufferedReader in) throws Exception {
        Field field = new Field();
        for (int i = 0; i < HEIGHT; ++i) {
            String[] columns = in.readLine().split(" ");
            for (int j = 0; j < WIDTH; ++j) {
                int b = Integer.parseInt(columns[j]);
                field.cell[(i + OFF_Y) * WIDTH_EX + (j + OFF_X)] = b;
                if (b == 5) {
                    field.fives++;
                }
                // if (b > 0 && field.tops[i] == 0) {
                    // field.tops[i] = i + OFF_Y;
                // }
                if (b > 0 && field.top == 0) {
                    field.top = i + OFF_Y;
                }
            }
        }
        for (int i = 0; i < WIDTH_EX; ++i) {
            field.cell[(HEIGHT_EX - 1) * WIDTH_EX + i] = OJAMA_CELL;
        }
        in.readLine(); // discard END
        return field;
    }

    static final int WIDTH = 10;
    static final int HEIGHT = 16;
    static final int DANGER_LINE = 17;
    static final int EMPTY_CELL = 0;
    static final int OJAMA_CELL = 11;
    static final int WIDTH_EX = WIDTH + 2;
    static final int HEIGHT_EX = HEIGHT + 4;
    static final int OFF_X = 1;
    static final int OFF_Y = 3;
    static final int OFF_DEAD_Y = 2;
    static final int FIELD_SIZE = WIDTH_EX * HEIGHT_EX;

    static final int[] DT = {
        -WIDTH_EX - 1, -WIDTH_EX, -WIDTH_EX + 1,
        -1, 1,
        WIDTH_EX - 1, WIDTH_EX, WIDTH_EX + 1
    };
    static final int[] ERDT = {
        -WIDTH_EX, -WIDTH_EX + 1, 1, WIDTH_EX + 1
    };

    int[] cell = new int[FIELD_SIZE];
    // int[] tops = new int[WIDTH_EX];
    int top = 0;
    int fives = 0;

    private Field() {}

    int get(int y, int x) {
        return cell[(y + OFF_Y) * WIDTH_EX + (x + OFF_X)];
    }

    boolean isEmpty(int y, int x) {
        return get(y, x) == EMPTY_CELL;
    }

    boolean containsFive() {
        return fives > 0;
    }

    boolean isDead() {
        for (int idx = OFF_DEAD_Y * WIDTH_EX + OFF_X, d = 0; d < WIDTH; ++d) {
            if (cell[idx + d] != EMPTY_CELL) {
                return true;
            }
        }
        return false;
    }

    Field getCopy() {
        Field field = new Field();
        field.cell = Arrays.copyOf(cell, cell.length);
        // field.tops = Arrays.copyOf(tops, tops.length);
        field.top = top;
        field.fives = fives;
        return field;
    }

    void putPack(Pack pack) {
        for (int x = 0; x < 2; ++x) {
            int idx = (HEIGHT_EX - 2) * WIDTH_EX + (pack.pos + OFF_X + x);
            while (cell[idx] > 0) { idx -= WIDTH_EX; }
            for (int y = 2; y >= 0; y -= 2) {
                int n = pack.get(y + x);
                if (n != 0) {
                    cell[idx] = n;
                    top = Math.min(top, idx / WIDTH_EX);
                    idx -= WIDTH_EX;
                }
            }
        }
    }

    boolean erase() {
        boolean[] flag = new boolean[cell.length];
        for (int idx = (HEIGHT_EX - 1) * WIDTH_EX - 2; idx > WIDTH_EX; --idx) {
            int c = cell[idx];
            for (int dt : ERDT) {
                if (c + cell[idx + dt] == 10) {
                    flag[idx] = true;
                    flag[idx + dt] = true;
                }
            }
        }
        boolean erased = false;
        for (int i = 0; i < flag.length; ++i) {
            if (flag[i]) {
                if (cell[i] == 5) {
                    --fives;
                }
                cell[i] = 0;
                erased = true;
            }
        }
        return erased;
    }

    boolean drop() {
        boolean droped = false;
        int top = HEIGHT_EX;
        for (int x = OFF_X; x <= WIDTH; ++x) {
            for (int idx = (HEIGHT_EX - 2) * WIDTH_EX + x; idx >= WIDTH_EX; idx -= WIDTH_EX) {
                if (cell[idx] != 0) { continue; }
                int emp = idx;
                for (idx -= WIDTH_EX; idx > 0; idx -= WIDTH_EX) {
                    if (cell[idx] == 0) { continue; }
                    cell[emp] = cell[idx];
                    cell[idx] = 0;
                    emp -= WIDTH_EX;
                    droped = true;
                }
                top = Math.min(top, emp / WIDTH_EX + 1);
                break;
            }
        }
        this.top = top;
        return droped;
    }
}

class Pack implements Command {

    static Pack scan(BufferedReader in) throws Exception {
        Pack pack = new Pack();
        String[] top = in.readLine().split(" ");
        String[] bottom = in.readLine().split(" ");
        in.readLine(); // discard END
        pack.block[0] = Integer.parseInt(top[0]);
        pack.block[1] = Integer.parseInt(top[1]);
        pack.block[2] = Integer.parseInt(bottom[0]);
        pack.block[3] = Integer.parseInt(bottom[1]);
        return pack;
    }

    static final int SIZE = 2;

    static final int[][] ROLLED_INDEXES;
    static {
        ROLLED_INDEXES = new int[4][SIZE * SIZE];
        for (int dy = 0; dy < SIZE; ++dy) {
            for (int dx = 0; dx < SIZE; ++dx) {
                ROLLED_INDEXES[0][dy * SIZE + dx] = dy * SIZE + dx;
            }
        }
        for (int i = 1; i < 4; ++i) {
            for (int dy = 0; dy < SIZE; ++dy) {
                for (int dx = 0; dx < SIZE; ++dx) {
                    ROLLED_INDEXES[i][dx * SIZE + (SIZE - 1 - dy)] =
                        ROLLED_INDEXES[i - 1][dy * SIZE + dx];
                }
            }
        }
    }

    int[] block = new int[SIZE * SIZE];
    int rot = 0;
    int pos = 0;

    private Pack() {}

    int get(int i) {
        return block[ROLLED_INDEXES[rot][i]];
    }

    int get(int y, int x) {
        return get(y * SIZE + x);
    }

    void rotate() {
        rot = (rot + 1) & 3;
    }

    @Override
    public String toString() {
        return String.format("%d %d", pos, rot);
    }
}

interface Skill extends Command {
    boolean canFire(State state);
    void fire(State state);
}

class Bomb implements Skill {
    static final int REQUIRE_GAUGE = 80;
    static final Bomb BOMB = new Bomb();
    private Bomb() {}
    public boolean canFire(State state) {
        if (state.gauge < REQUIRE_GAUGE) {
            return false;
        }
        return state.field.containsFive();
    }
    public void fire(State state) {

    }

    @Override
    public String toString() {
        return "S";
    }
}

class MyAI implements AI {

    static final String VERSION = "v0.4.0";
    static final String NAME = "LeonardoneAI";

    static final PrintStream err = System.err;

    public String getName() { return NAME + VERSION; }

    Pack[] packs = null;

    public void init(Pack[] packs) throws Exception {
        this.packs = packs;
    }

    public Command getCommand(Turn turn) throws Exception {
        Pack pack = packs[turn.getCount()];
        State my = turn.getMyState();
        Field field = my.field;

        if (Bomb.BOMB.canFire(my)) {
            return Bomb.BOMB;
        }

        int bestRot = 0;
        int bestX = 0;
        int bestTop = 0;
        long bestScore = 0;
        for (int rot = 0; rot < 4; ++rot) {
            pack.rot = rot;
            for (int x = 0; x <= 8; ++x) {
                pack.pos = x;
                State tmp = my.getCopy();
                tmp.putPack(pack);
                if (tmp.isGameOver()) {
                    continue;
                }
                if (tmp.score > bestScore ||
                    (tmp.score == bestScore &&
                        tmp.field.top > bestTop)) {
                    bestRot = rot;
                    bestX = x;
                    bestTop = tmp.field.top;
                    bestScore = tmp.score;
                }
            }
        }

        pack.rot = bestRot;
        pack.pos = bestX;

        return pack;
    }

    void printP(Pack pack) {
        for (int y = 0; y < Pack.SIZE; ++y) {
            for (int x = 0; x < pack.pos; ++x) {
                err.print("  ");
            }
            for (int x = 0; x < Pack.SIZE; ++x) {
                err.printf("%d ", pack.get(y, x));
            }
            err.println();
        }
    }

    void printF(Field field) {
        for (int y = 0; y < Field.HEIGHT; ++y) {
            for (int x = 0; x < Field.WIDTH; ++x) {
                err.printf("%d ", field.get(y, x));
            }
            err.println();
        }
    }

    void printL() {
        err.println("---------------");
    }

}

