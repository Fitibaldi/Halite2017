package nn.example;

import nn.entity.Result;
import nn.utils.DataUtils;
import nn.NeuralNetwork;
import nn.entity.Error;
import nn.callback.INeuralNetworkCallback;

/**
 * @author jlmd
 */
public class Main {
    public static void main(String [] args){
        System.out.println("Starting neural network sample... ");

        float[][] x = DataUtils.readInputsFromFile("data/x.txt");
        int[] t = DataUtils.readOutputsFromFile("data/t.txt");

        NeuralNetwork neuralNetwork = new NeuralNetwork(x, t, new INeuralNetworkCallback() {
            @Override
            public void success(Result result) {
                float[] valueToPredict = new float[] {-0.205f, 0.980f};
                System.out.println("Success percentage: " + result.getSuccessPercentage());
                System.out.println("Predicted result: " + result.predictValue(valueToPredict));
            }

            @Override
            public void failure(Error error) {
                System.out.println("Error: " + error.getDescription());
            }
        });

        neuralNetwork.startLearning();
    }
}
