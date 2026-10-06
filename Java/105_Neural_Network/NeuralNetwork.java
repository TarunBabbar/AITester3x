import java.util.Random;

/**
 * 105 - A small neural network from scratch: two inputs, a hidden layer, one
 * output, trained by backpropagation with gradient descent. No libraries.
 *
 * XOR is the classic test: it cannot be solved by a single layer, so the network
 * only learns it once the hidden layer is there.
 *
 * Compile and run:
 *   javac NeuralNetwork.java
 *   java NeuralNetwork
 */
public class NeuralNetwork {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static double sigmoid(double value) {
        return 1.0 / (1.0 + Math.exp(-value));
    }

    /** The derivative expressed in terms of the sigmoid's own output. */
    static double sigmoidDerivative(double output) {
        return output * (1 - output);
    }

    static final class Network {
        private final int inputs;
        private final int hidden;
        private final double[][] hiddenWeights;   // [hidden neuron][input]
        private final double[] hiddenBiases;
        private final double[] outputWeights;     // [hidden neuron]
        private double outputBias;
        private double firstLoss = -1;
        private double lastLoss = -1;

        Network(int inputs, int hidden, Random random) {
            this.inputs = inputs;
            this.hidden = hidden;
            this.hiddenWeights = new double[hidden][inputs];
            this.hiddenBiases = new double[hidden];
            this.outputWeights = new double[hidden];
            for (int neuron = 0; neuron < hidden; neuron++) {
                for (int input = 0; input < inputs; input++) {
                    hiddenWeights[neuron][input] = random.nextDouble() - 0.5;
                }
                hiddenBiases[neuron] = random.nextDouble() - 0.5;
                outputWeights[neuron] = random.nextDouble() - 0.5;
            }
            outputBias = random.nextDouble() - 0.5;
        }

        record Activations(double[] hidden, double output) {
        }

        Activations forward(double[] input) {
            double[] hiddenValues = new double[hidden];
            for (int neuron = 0; neuron < hidden; neuron++) {
                double sum = hiddenBiases[neuron];
                for (int i = 0; i < inputs; i++) {
                    sum += hiddenWeights[neuron][i] * input[i];
                }
                hiddenValues[neuron] = sigmoid(sum);
            }
            double sum = outputBias;
            for (int neuron = 0; neuron < hidden; neuron++) {
                sum += outputWeights[neuron] * hiddenValues[neuron];
            }
            return new Activations(hiddenValues, sigmoid(sum));
        }

        /** One pass over the samples per epoch, returning the final total loss. */
        double train(double[][] samples, double[] expected, double learningRate, int epochs) {
            for (int epoch = 0; epoch < epochs; epoch++) {
                double totalLoss = 0;
                for (int sample = 0; sample < samples.length; sample++) {
                    Activations activations = forward(samples[sample]);
                    double error = activations.output() - expected[sample];
                    totalLoss += error * error / 2;

                    double outputDelta = error * sigmoidDerivative(activations.output());

                    // The hidden deltas need the output weights as they were before
                    // this update, so keep a copy.
                    double[] previousOutputWeights = outputWeights.clone();
                    for (int neuron = 0; neuron < hidden; neuron++) {
                        outputWeights[neuron] -= learningRate * outputDelta * activations.hidden()[neuron];
                    }
                    outputBias -= learningRate * outputDelta;

                    for (int neuron = 0; neuron < hidden; neuron++) {
                        double hiddenDelta = sigmoidDerivative(activations.hidden()[neuron])
                                * outputDelta * previousOutputWeights[neuron];
                        for (int i = 0; i < inputs; i++) {
                            hiddenWeights[neuron][i] -= learningRate * hiddenDelta * samples[sample][i];
                        }
                        hiddenBiases[neuron] -= learningRate * hiddenDelta;
                    }
                }
                if (epoch == 0) {
                    firstLoss = totalLoss;
                }
                lastLoss = totalLoss;
            }
            return lastLoss;
        }

        double firstLoss() {
            return firstLoss;
        }

        double lastLoss() {
            return lastLoss;
        }

        double predict(double[] input) {
            return forward(input).output();
        }

        int hiddenUnits() {
            return hidden;
        }

        /** Every weight and bias, for comparing two networks. */
        double[] parameters() {
            double[] all = new double[hidden * inputs + hidden + hidden + 1];
            int index = 0;
            for (double[] row : hiddenWeights) {
                for (double weight : row) {
                    all[index++] = weight;
                }
            }
            for (double bias : hiddenBiases) {
                all[index++] = bias;
            }
            for (double weight : outputWeights) {
                all[index++] = weight;
            }
            all[index] = outputBias;
            return all;
        }
    }

    static void checkAllOutputs(Network network, double[][] inputs, double[] expected, double tolerance) {
        for (int sample = 0; sample < inputs.length; sample++) {
            double predicted = network.predict(inputs[sample]);
            check(Math.abs(predicted - expected[sample]) < tolerance,
                    "sample " + sample + " gave " + predicted + ", expected " + expected[sample]);
        }
    }

    public static void main(String[] args) {
        // ---- the activation function ------------------------------------------
        check(Math.abs(sigmoid(0) - 0.5) < 1e-12, "sigmoid(0) is one half");
        check(sigmoid(100) > 0.99 && sigmoid(-100) < 0.01, "it squashes to 0 and 1");
        check(sigmoidDerivative(0.5) == 0.25, "the derivative at one half is a quarter");
        check(Math.abs((sigmoid(2) + sigmoid(-2)) - 1.0) < 1e-12, "and it is symmetric about a half");
        System.out.printf("sigmoid      : s(0)=%.3f s(2)=%.3f s(-2)=%.3f%n",
                sigmoid(0), sigmoid(2), sigmoid(-2));

        // ---- the XOR problem --------------------------------------------------
        double[][] xorInputs = {{0, 0}, {0, 1}, {1, 0}, {1, 1}};
        double[] xorExpected = {0, 1, 1, 0};

        Network untrained = new Network(2, 4, new Random(42));
        double beforeTraining = untrained.predict(xorInputs[1]);
        check(beforeTraining > 0 && beforeTraining < 1, "an untrained network still outputs a probability");
        System.out.printf("untrained    : XOR(0,1) = %.4f (wanted 1)%n", beforeTraining);

        Network trained = new Network(2, 4, new Random(42));
        double loss = trained.train(xorInputs, xorExpected, 0.5, 20_000);
        checkAllOutputs(trained, xorInputs, xorExpected, 0.1);
        check(loss < trained.firstLoss(), "training reduced the loss");
        check(trained.lastLoss() < 0.01, "and it is small at the end: " + trained.lastLoss());
        System.out.printf("trained      : loss %.6f -> %.6f over 20,000 epochs%n",
                trained.firstLoss(), trained.lastLoss());
        for (int sample = 0; sample < xorInputs.length; sample++) {
            System.out.printf("               %d XOR %d -> %.4f (wanted %d)%n",
                    (int) xorInputs[sample][0], (int) xorInputs[sample][1],
                    trained.predict(xorInputs[sample]), (int) xorExpected[sample]);
        }

        // ---- the hidden layer is what makes it possible -----------------------
        // A network with no hidden units can only draw a straight line, and XOR
        // is not linearly separable.
        Network linearOnly = new Network(2, 1, new Random(42));
        linearOnly.train(xorInputs, xorExpected, 0.5, 20_000);
        double worstError = 0;
        for (int sample = 0; sample < xorInputs.length; sample++) {
            worstError = Math.max(worstError, Math.abs(linearOnly.predict(xorInputs[sample])
                    - xorExpected[sample]));
        }
        check(worstError > 0.2, "one hidden unit cannot fit XOR: worst error " + worstError);
        System.out.printf("one unit     : worst error %.3f, so XOR needs the hidden layer%n", worstError);

        // ---- other gates are easy by comparison -------------------------------
        double[][] andInputs = {{0, 0}, {0, 1}, {1, 0}, {1, 1}};
        double[] andExpected = {0, 0, 0, 1};
        double[] orExpected = {0, 1, 1, 1};

        Network andGate = new Network(2, 2, new Random(1));
        andGate.train(andInputs, andExpected, 0.5, 10_000);
        checkAllOutputs(andGate, andInputs, andExpected, 0.1);

        Network orGate = new Network(2, 2, new Random(1));
        orGate.train(andInputs, orExpected, 0.5, 10_000);
        checkAllOutputs(orGate, andInputs, orExpected, 0.1);
        System.out.println("gates        : AND and OR both learned");

        // ---- determinism ------------------------------------------------------
        Network firstRun = new Network(2, 4, new Random(7));
        Network secondRun = new Network(2, 4, new Random(7));
        firstRun.train(xorInputs, xorExpected, 0.5, 5_000);
        secondRun.train(xorInputs, xorExpected, 0.5, 5_000);
        double[] firstParameters = firstRun.parameters();
        double[] secondParameters = secondRun.parameters();
        check(firstParameters.length == secondParameters.length, "the same number of parameters");
        for (int i = 0; i < firstParameters.length; i++) {
            check(firstParameters[i] == secondParameters[i],
                    "parameter " + i + " differs between identical runs");
        }
        System.out.println("determinism  : the same seed gives byte-identical weights");

        // a different seed arrives at a different solution
        Network otherSeed = new Network(2, 4, new Random(99));
        otherSeed.train(xorInputs, xorExpected, 0.5, 5_000);
        boolean differs = false;
        double[] otherParameters = otherSeed.parameters();
        for (int i = 0; i < firstParameters.length; i++) {
            if (firstParameters[i] != otherParameters[i]) {
                differs = true;
                break;
            }
        }
        check(differs, "a different seed gives different weights");
        checkAllOutputs(otherSeed, xorInputs, xorExpected, 0.1);
        System.out.println("seeds        : a different seed still solves XOR, with other weights");

        // ---- more epochs keep helping, up to a point --------------------------
        Network brief = new Network(2, 4, new Random(5));
        brief.train(xorInputs, xorExpected, 0.5, 50);
        Network longRun = new Network(2, 4, new Random(5));
        longRun.train(xorInputs, xorExpected, 0.5, 10_000);
        check(longRun.lastLoss() < brief.lastLoss(),
                "10,000 epochs beat 50: " + longRun.lastLoss() + " against " + brief.lastLoss());
        System.out.printf("epochs       : 50 gives loss %.4f, 10,000 gives %.6f%n",
                brief.lastLoss(), longRun.lastLoss());

        // ---- every prediction is a probability --------------------------------
        for (double[] input : new double[][] {{0, 0}, {0, 1}, {1, 0}, {1, 1}, {0.5, 0.5}}) {
            double probability = trained.predict(input);
            check(probability > 0 && probability < 1, "the sigmoid output is a probability");
        }
        double half = trained.predict(new double[] {0.5, 0.5});
        System.out.printf("midpoint     : XOR(0.5,0.5) = %.4f%n", half);

        // ---- the shape of the network -----------------------------------------
        Network shape = new Network(3, 5, new Random(0));
        check(shape.hiddenUnits() == 5, "the hidden width is what was asked for");
        check(shape.parameters().length == 5 * 3 + 5 + 5 + 1,
                "the parameter count: weights, biases, output weights and the output bias");
        System.out.println("shape        : 3 inputs, 5 hidden units, "
                + shape.parameters().length + " parameters");
        System.out.println("All checks passed.");
    }
}
