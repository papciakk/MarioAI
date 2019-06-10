package pl.edu.agh.ai.learning;

import com.grapeshot.halfnes.NES;
import com.grapeshot.halfnes.State;
import com.grapeshot.halfnes.ui.PuppetController;
import com.grapeshot.halfnes.ui.PuppetController.Button;
import pl.edu.agh.ai.nes.game.SuperMarioBrosGame;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class Learning {
    private State state;
    private NES nes;
    private SuperMarioBrosGame game;
    private int stepsCounter;
    private PuppetController controller;

    private boolean randomRun = false;
    private double currentScore = 0;
    private double lastScore = 0;
    private int stuckCounter = 0;
    private long frameStartTime;
    private double scoreDiff;
    private boolean stuck = false;
    private int lastPos = 0, currentPos = 0;

    private NesMDP nesMDP;

    private static final int STUCK_FRAMES = 400;
    private static final double DEATH_PENALTY = -100000.0;
    private static final double SUCCESS_REWARD = 1000000.0;

    enum ActionSpace {
        NOOP(new Button[]{}),
        RIGHT(new Button[]{Button.RIGHT}),
        RIGHT_A(new Button[]{Button.RIGHT, Button.A}),
        RIGHT_B(new Button[]{Button.RIGHT, Button.B}),
        RIGHT_A_B(new Button[]{Button.RIGHT, Button.A, Button.B}),
        A(new Button[]{Button.A}),
        LEFT(new Button[]{Button.LEFT}),
        LEFT_A(new Button[]{Button.LEFT, Button.A}),
        LEFT_B(new Button[]{Button.LEFT, Button.B}),
        LEFT_A_B(new Button[]{Button.LEFT, Button.A, Button.B}),
        DOWN(new Button[]{Button.DOWN}),
        UP(new Button[]{Button.UP});

        private Button[] buttonAction;

        ActionSpace(Button[] buttons) {
            buttonAction = buttons;
        }

        public Button[] getButtonAction() {
            return buttonAction;
        }

    }

    public Learning(NES nes) {
        this.nes = nes;
        loadState("mario_start.sav");
        stepsCounter = 0;
    }

    private void loadState(String filename) {
        try {
            FileInputStream fileIn = new FileInputStream(filename);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            state = (State) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("State class not found");
            c.printStackTrace();
        }
    }

    public void init() {
        initNESEmulator();
        game = new SuperMarioBrosGame(nes.getCPURAM());
    }

    public void setNesMDP(NesMDP nesMDP) {
        this.nesMDP = nesMDP;
    }

    private void initNESEmulator() {
        nes.loadROM("mario.nes");
        loadStartingState();
        controller = (PuppetController) nes.getcontroller1();
    }

    public void step() {
        if (!nes.isRunEmulation()) return;

        if (stepsCounter == 0) {
            reload();
        } else {
            scoreDiff = currentScore - lastScore;
            currentScore = game.getScore();
            currentPos = game.getPlayerAbsoluteX();


            if (stepsCounter % 10 == 0) {
                lastPos = currentPos;
            }

            if (stepsCounter % 2 == 0) {
                lastScore = currentScore;
            }

            checkIfIsStuck();
        }

        stepsCounter++;
    }

    private void checkIfIsStuck() {
        if (Math.abs(currentPos - lastPos) < 5) {
            stuckCounter++;
        } else {
            stuckCounter = 0;
        }

        if (stuckCounter == STUCK_FRAMES) {
            stuck = true;
        }
    }

    public void reload() {
        loadStartingState();
        currentScore = 0;
        lastScore = 0;
        stuckCounter = 0;
        stuck = false;
        lastPos = 0;
        currentPos = 0;
    }

    private void loadStartingState() {
        nes.setState(state);
    }

    void clearMoves() {
        for (PuppetController.Button btn : PuppetController.Button.values()) {
            controller.releaseButton(btn);
        }
    }

    public void pressButtons(Button[] buttons) {
        for (Button btn : buttons) {
            controller.pressButton(btn);
        }
    }

    public void enableRandomRun() {
        randomRun = true;
        reload();
    }

    public void nesStep() {
        if (nes.isRunEmulation()) {
            frameStartTime = System.nanoTime();
            nes.runframe();
//            step();
            nes.setFrameTime(System.nanoTime() - frameStartTime);
        } else {
            nes.renderGui();
        }
    }

    public void run() {
        while (!nes.isShutdown()) {
            step();
            nesStep();
        }
    }

    public void shutdown() {
        nes.setShutdown(true);
    }

    public GameObservation getObservation() {
        Integer[] learningArray = nes.getLearningArray();
        if (learningArray == null) {
            Integer[] emptyObservation = new Integer[32 * 30];
            for (int i = 0; i < emptyObservation.length; i++) {
                emptyObservation[i] = 0;
            }
            return new GameObservation(emptyObservation);
        } else {
            return new GameObservation(learningArray);
        }
    }

    public boolean isDone() {
        return game.fail() || game.success() || isStuck();
    }

    public int getActionSpaceSize() {
        return ActionSpace.values().length;
    }

    public Button[] chooseAction(int actionId) {
        return ActionSpace.values()[actionId].getButtonAction();
    }

    public String actionIdToString(int actionId) {
        return ActionSpace.values()[actionId].name();
    }

    public boolean isStuck() {
        return stuck;
    }

    public double getReward() {
        if (!isDone()) {
            return scoreDiff;
        } else {
            if (game.fail() || isStuck()) {
                return DEATH_PENALTY;
            }

            if (game.success()) {
                return SUCCESS_REWARD;
            }
        }
        return 0.0;
//        return new Random().nextInt(10) - 5;
    }

    public NES getNES() {
        return nes;
    }

}
