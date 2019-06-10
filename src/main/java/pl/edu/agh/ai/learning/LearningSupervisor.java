package pl.edu.agh.ai.learning;

import com.grapeshot.halfnes.NES;
import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning;
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense;
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense;
import org.deeplearning4j.rl4j.policy.DQNPolicy;
import org.deeplearning4j.rl4j.util.DataManager;
import org.nd4j.linalg.learning.config.Adam;

import java.io.IOException;

public class LearningSupervisor {

    private Learning learning;
    private NesMDP nesMDP;
    private NES nes;

    public QLearning.QLConfiguration QL = new QLearning.QLConfiguration(
            9999,
            50000,
            1000000,
            200000,
            32,
            50,
            10,
            0.001,
            0.99,
            1.0,
            0.05f,
            1000,
            true
    );

    public DQNFactoryStdDense.Configuration NET =
            DQNFactoryStdDense.Configuration.builder()
                    .l2(0.001)
                    .updater(new Adam(0.005))
                    .numHiddenNodes(50)
                    .numLayer(3)
                    .build();

    public LearningSupervisor(NES nes) {
        this.nes = nes;
        learning = new Learning(nes);
        nesMDP = new NesMDP(learning);
        learning.setNesMDP(nesMDP);
    }

    public void init() {
        learning.init();
//        nes.setLearning(learning);
    }

    public void doLearning() throws IOException {
        DataManager manager = new DataManager(true);

        QLearningDiscreteDense<GameObservation> dql =
                new QLearningDiscreteDense<GameObservation>(nesMDP, NET, QL, manager);

        dql.train();

        DQNPolicy<GameObservation> policy = dql.getPolicy();
        policy.save("policy.dat");

        nesMDP.close();
    }

    public Learning getLearning() {
        return learning;
    }

    public void startLearning() {
        new Thread(() -> {
            try {
                doLearning();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

    }

    public void loadState() throws IOException {
        DQNPolicy<GameObservation> policy = DQNPolicy.load("policy.dat");

        new Thread(() -> {
            for(int i = 0; i < 10; i++) {
                nesMDP.reset();
                double reward = policy.play(nesMDP);
                System.out.println(reward);
            }
        }).start();
        nesMDP.close();
    }
}
