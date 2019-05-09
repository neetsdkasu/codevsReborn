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

    State getOppnentState() {
        return states[1];
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
        state.dropOjama();
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

    static final int[] GAUGE_ATTACK = new int[50];
    static {
        for (int i = 3; i < GAUGE_ATTACK.length; ++i) {
            GAUGE_ATTACK[i] = 12 + 2 * i;
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
        if (!field.putPack(pack)) {
            return -1;
        }
        return checkChain();
    }

    int checkChain() {
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

    void dropOjama() {
        if (stock < Field.WIDTH) {
            return;
        }
        stock -= Field.WIDTH;
        field.putOjama();
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
    static final int HEIGHT_EX = HEIGHT + 5;
    static final int OFF_X = 1;
    static final int OFF_Y = 4;
    static final int OFF_DEAD_Y = 3;
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
        return top <= OFF_DEAD_Y;
    }

    Field getCopy() {
        Field field = new Field();
        field.cell = Arrays.copyOf(cell, cell.length);
        // field.tops = Arrays.copyOf(tops, tops.length);
        field.top = top;
        field.fives = fives;
        return field;
    }

    boolean putPack(Pack pack) {
        for (int x = 0; x < 2; ++x) {
            int idx = (HEIGHT_EX - 2) * WIDTH_EX + (pack.pos + OFF_X + x);
            while (idx > 0 && cell[idx] > 0) { idx -= WIDTH_EX; }
            for (int y = 2; y >= 0; y -= 2) {
                int n = pack.get(y + x);
                if (n != 0) {
                    if (idx < 0) { return false; }
                    cell[idx] = n;
                    top = Math.min(top, idx / WIDTH_EX);
                    idx -= WIDTH_EX;
                }
            }
        }
        return true;
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

    void putOjama() {
        for (int x = OFF_X; x <= WIDTH; ++x) {
            for (int idx = (HEIGHT_EX - 2) * WIDTH_EX + x; idx > WIDTH_EX; idx -= WIDTH_EX) {
                if (cell[idx] != EMPTY_CELL) {
                    continue;
                }
                cell[idx] = OJAMA_CELL;
                top = Math.min(top, idx / WIDTH_EX);
                break;
            }
        }
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
    int fire(State state);
}

class Bomb implements Skill {
    static final int REQUIRE_GAUGE = 80;
    static final int TRIGGER = 5;

    static final Bomb BOMB = new Bomb();
    static final int[] BOMB_SCORES = new int[Field.WIDTH * Field.HEIGHT + 1];
    static {
        for (int i = 1; i < BOMB_SCORES.length; ++i) {
            BOMB_SCORES[i] = (int)Math.floor(25.0 * Math.pow(2.0, (double)i / 12.0));
        }
    }

    private Bomb() {}
    public boolean canFire(State state) {
        if (state.gauge < REQUIRE_GAUGE) {
            return false;
        }
        return state.field.containsFive();
    }

    public int fire(State state) {
        Field field = state.field;
        int bs = destroy(field);
        state.stock -= bs / 2;
        state.score += bs;
        if (field.drop()) {
            return state.checkChain();
        }
        return 0;
    }

    int destroy(Field field) {
        int[] cell = field.cell;
        boolean[] flag = new boolean[cell.length];
        for (int idx = (Field.HEIGHT_EX - 1) * Field.WIDTH_EX - 2; idx > Field.WIDTH_EX; --idx) {
            if (cell[idx] != TRIGGER) { continue; }
            for (int dt : Field.DT) {
                flag[idx + dt] = true;
            }
        }
        field.fives = 0;
        int count = 0;
        for (int i = 0; i < flag.length; ++i) {
            if (flag[i]) {
                int c = cell[i];
                if (c == Field.EMPTY_CELL || c == Field.OJAMA_CELL) {
                    continue;
                }
                cell[i] = 0;
                ++count;
            }
        }
        return BOMB_SCORES[count];
    }

    @Override
    public String toString() {
        return "S";
    }
}

class Item {
    State state = null;
    boolean skill = false;
    int rot = 0, pos = 0, attackG = 0;
    public Item(State state, boolean skill, int rot, int pos, int attackG) {
        this.state = state;
        this.skill = skill;
        this.rot = rot;
        this.pos = pos;
        this.attackG = attackG;
    }
}

class MyAI implements AI {

    static final String VERSION = "v0.7.0";
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

        Item opp = getBest(turn.getCount(), turn.getOppnentState());
        int attackS = 0, attackG = 0;
        if (opp != null) {
            if (opp.state.stock < 0) {
                attackS = -opp.state.stock;
            }
            attackG = opp.attackG;
        }

        boolean skill = false;
        int bestRot = 0;
        int bestX = 0;
        int bestTop = 0;
        int bestScore = 0;
        State best = my;

        if (Bomb.BOMB.canFire(my)) {
            State tmp = my.getCopy();
            Bomb.BOMB.fire(tmp);
            if (!tmp.isGameOver()) {
                tmp.stock += attackS;
                tmp.gauge -= attackG;
                tmp.dropOjama();
                Item item = getBest(turn.getCount() + 1, tmp);
                if (item != null) {
                    tmp = item.state;
                }
                if (!tmp.isGameOver()) {
                    skill = true;
                    bestTop = tmp.field.top;
                    bestScore = tmp.score;
                    best = tmp;
                }
            }
        }

        for (int rot = 0; rot < 4; ++rot) {
            pack.rot = rot;
            for (int x = 0; x <= 8; ++x) {
                pack.pos = x;
                State tmp = my.getCopy();
                if (tmp.putPack(pack) < 0) {
                    continue;
                }
                if (tmp.isGameOver()) {
                    continue;
                }
                tmp.stock += attackS;
                tmp.gauge -= attackG;
                tmp.dropOjama();
                Item item = getBest(turn.getCount() + 1, tmp);
                if (item != null) {
                    tmp = item.state;
                    if (tmp.isGameOver()) {
                        continue;
                    }
                }
                if (tmp.score > bestScore ||
                    (tmp.score == bestScore &&
                        tmp.field.top > bestTop)) {
                    skill = false;
                    bestRot = rot;
                    bestX = x;
                    bestTop = tmp.field.top;
                    bestScore = tmp.score;
                    best = tmp;
                }
            }
        }

        pack.rot = bestRot;
        pack.pos = bestX;

        // printL();
        // printL();
        // err.println("turn: " + turn.getCount());
        // err.println("go:" + my.isGameOver());
        // err.println("skill: " + skill);
        // printP(pack);
        // printF(my.field);
        // printL();
        // err.println("future");
        // printF(best.field);

        if (skill) {
            return Bomb.BOMB;
        }

        return pack;
    }

    Item getBest(int turnCount, State state) {
        if (turnCount >= packs.length) {
            return null;
        }

        Pack pack = packs[turnCount];

        boolean skill = false;
        int bestRot = 0;
        int bestX = 0;
        int bestTop = 0;
        int bestScore = -1;
        int bestAttackG = 0;
        State best = state;

        if (Bomb.BOMB.canFire(state)) {
            State tmp = state.getCopy();
            int chain = Bomb.BOMB.fire(tmp);
            if (!tmp.isGameOver()) {
                skill = true;
                bestTop = tmp.field.top;
                bestScore = tmp.score;
                bestAttackG = State.GAUGE_ATTACK[chain];
                best = tmp;
            }
        }

        for (int rot = 0; rot < 4; ++rot) {
            pack.rot = rot;
            for (int x = 0; x <= 8; ++x) {
                pack.pos = x;
                State tmp = state.getCopy();
                int chain = tmp.putPack(pack);
                if (chain < 0) {
                    continue;
                }
                if (tmp.isGameOver()) {
                    continue;
                }
                if (tmp.score > bestScore ||
                    (tmp.score == bestScore &&
                        tmp.field.top > bestTop)) {
                    skill = false;
                    bestRot = rot;
                    bestX = x;
                    bestTop = tmp.field.top;
                    bestScore = tmp.score;
                    bestAttackG = State.GAUGE_ATTACK[chain];
                    best = tmp;
                }
            }
        }

        if (bestScore < 0) {
            return null;
        }

        return new Item(best, skill, bestRot, bestX, bestAttackG);
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
        for (int y = -2; y < Field.HEIGHT; ++y) {
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

