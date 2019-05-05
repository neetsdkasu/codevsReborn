// CODE VS - Reborn
// author: Leonrdone @ NEETSDKASU
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;

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
        state.stock = Long.parseLong(in.readLine());
        state.gauge = Integer.parseInt(in.readLine());
        state.score = Long.parseLong(in.readLine());
        state.field = Field.scan(in);
        return state;
    }

    long timeLeft, stock, score;
    int gauge;
    Field field;

    private State() {}

}

class Field {

    static Field scan(BufferedReader in) throws Exception {
        Field field = new Field();
        for (int i = 0; i < HEIGHT; ++i) {
            String[] columns = in.readLine().split(" ");
            for (int j = 0; j < WIDTH; ++j) {
                field.cell[i * WIDTH + j] = Integer.parseInt(columns[j]);
            }
        }
        in.readLine(); // discard END
        return field;
    }

    static final int WIDTH = 10;
    static final int HEIGHT = 16;
    static final int DANGER_LINE = 17;
    static final int EMPTY_CELL = 0;

    int[] cell = new int[WIDTH * HEIGHT];

    private Field() {}

    int get(int y, int x) {
        return cell[y * WIDTH + x];
    }

    boolean isEmpty(int y, int x) {
        return get(y, x) == EMPTY_CELL;
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
        for (int i = 1; i < 4; i++) {
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
}

class Bomb implements Skill {
    static final int REQUIRE_GAUGE = 80;
    static final Bomb BOMB = new Bomb();
    private Bomb() {}
    public boolean canFire(State state) {
        if (state.gauge < REQUIRE_GAUGE) {
            return false;
        }
        int count = 0;
        for (int c : state.field.cell) {
            if (c == 5) {
                ++count;
            }
        }
        return count > 0;
    }
    @Override
    public String toString() {
        return "S";
    }
}

class MyAI implements AI {

    static final String VERSION = "v0.2.0";
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

        int[] ys = new int[Field.WIDTH];
        for (int x = 0; x < Field.WIDTH; ++x) {
            for (int y = Field.HEIGHT - 1; y > 0; --y) {
                if (field.isEmpty(y, x)) {
                    ys[x] = y;
                    break;
                }
            }
        }
        int bestX = 0;
        int bestY = 0;
        for (int x = 0; x <= 8; x++) {
            int y = Math.min(ys[x], ys[x + 1]);
            if (y > bestY) {
                bestX = x;
                bestY = y;
            }
        }

        pack.pos = bestX;

        return pack;
    }
}

