package pl.edu.agh.ai.learning;

import org.deeplearning4j.rl4j.space.Encodable;

public class GameObservation implements Encodable {

    private double[] arr;

    public GameObservation(Integer[] observationArray) {
        arr = new double[observationArray.length];
        for (int i = 0; i < observationArray.length; i++) {
            arr[i] = (double) observationArray[i];
        }
    }

    @Override
    public double[] toArray() {
        return arr;
    }
}
