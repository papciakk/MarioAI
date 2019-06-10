package pl.edu.agh.ai.learning;

import com.grapeshot.halfnes.ui.PuppetController;
import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.mdp.MDP;
import org.deeplearning4j.rl4j.space.ActionSpace;
import org.deeplearning4j.rl4j.space.ArrayObservationSpace;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import org.deeplearning4j.rl4j.space.ObservationSpace;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class NesMDP<O, A, AS extends ActionSpace<A>> implements MDP<GameObservation, Integer, DiscreteSpace> {
    private Learning learning;

    private ObservationSpace<GameObservation> observationSpace;
    private DiscreteSpace actionSpace;

    private BufferedWriter rewardWriter;

    private double totalReward = 0;

    public NesMDP(Learning learning) {
        this.learning = learning;
        observationSpace = new ArrayObservationSpace<>(new int[]{32 * 30});
        actionSpace = new DiscreteSpace(learning.getActionSpaceSize());

        rewardWriter = null;
        try {
            rewardWriter = new BufferedWriter(new FileWriter("rewards.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ObservationSpace<GameObservation> getObservationSpace() {
        return observationSpace;
    }

    @Override
    public DiscreteSpace getActionSpace() {
        return actionSpace;
    }

    @Override
    public GameObservation reset() {
        learning.reload();
        try {
            rewardWriter.write(totalReward + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        totalReward = 0;
        return learning.getObservation();
    }

    @Override
    public void close() {
        learning.getNES().setShutdown(true);
        try {
            rewardWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public StepReply<GameObservation> step(Integer actionId) {
        if(learning.getNES().isRunEmulation()) {
            PuppetController.Button[] actionButtons = learning.chooseAction(actionId);

            learning.clearMoves();
            learning.pressButtons(actionButtons);

            String actionName = learning.actionIdToString(actionId);

            learning.step();
            learning.nesStep();

            double reward = learning.getReward();
            totalReward += reward;
//            System.out.println(reward + " " + actionName);

            return new StepReply<>(learning.getObservation(), reward, learning.isDone(), null);
        } else {
            return new StepReply<>(learning.getObservation(), 0, learning.isDone(), null);
        }
    }

    @Override
    public boolean isDone() {
        return learning.isDone();
    }

    @Override
    public MDP<GameObservation, Integer, DiscreteSpace> newInstance() {
        return new NesMDP<GameObservation, Integer, DiscreteSpace>(learning);
    }
}
