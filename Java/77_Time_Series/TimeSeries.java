import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 77 - Time series: moving averages, exponential smoothing, z-score anomaly
 * detection, a least squares trend line and bucketing samples by hour.
 *
 * Compile and run:
 *   javac TimeSeries.java
 *   java TimeSeries
 */
public class TimeSeries {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    record Sample(LocalDateTime at, double value) {
    }

    /** Averages of each complete window, so the result is shorter than the input. */
    static List<Double> movingAverage(List<Double> values, int window) {
        if (window < 1) {
            throw new IllegalArgumentException("window must be at least 1");
        }
        List<Double> averages = new ArrayList<>();
        double running = 0;
        for (int i = 0; i < values.size(); i++) {
            running += values.get(i);
            if (i >= window) {
                running -= values.get(i - window);
            }
            if (i >= window - 1) {
                averages.add(running / window);
            }
        }
        return averages;
    }

    /** Exponential smoothing: each point blends the previous estimate with the new value. */
    static List<Double> exponentialMovingAverage(List<Double> values, double alpha) {
        if (alpha <= 0 || alpha > 1) {
            throw new IllegalArgumentException("alpha must be in (0, 1], got " + alpha);
        }
        List<Double> smoothed = new ArrayList<>();
        if (values.isEmpty()) {
            return smoothed;
        }
        double current = values.get(0);
        smoothed.add(current);
        for (int i = 1; i < values.size(); i++) {
            current = alpha * values.get(i) + (1 - alpha) * current;
            smoothed.add(current);
        }
        return smoothed;
    }

    static double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    /** Population standard deviation: divide by n, not n-1. */
    static double standardDeviation(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        double mean = mean(values);
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .sum() / values.size();
        return Math.sqrt(variance);
    }

    /** Indexes whose z-score is further from zero than the threshold. */
    static List<Integer> anomalies(List<Double> values, double threshold) {
        double mean = mean(values);
        double deviation = standardDeviation(values);
        List<Integer> flagged = new ArrayList<>();
        if (deviation == 0) {
            return flagged;      // every value identical: nothing can stand out
        }
        for (int i = 0; i < values.size(); i++) {
            double z = (values.get(i) - mean) / deviation;
            if (Math.abs(z) > threshold) {
                flagged.add(i);
            }
        }
        return flagged;
    }

    static double zScore(List<Double> values, int index) {
        double deviation = standardDeviation(values);
        return deviation == 0 ? 0 : (values.get(index) - mean(values)) / deviation;
    }

    /** Least squares slope: how much the value changes per step. */
    static double slope(List<Double> values) {
        int n = values.size();
        if (n < 2) {
            return 0;
        }
        double meanX = (n - 1) / 2.0;
        double meanY = mean(values);
        double numerator = 0;
        double denominator = 0;
        for (int i = 0; i < n; i++) {
            double dx = i - meanX;
            numerator += dx * (values.get(i) - meanY);
            denominator += dx * dx;
        }
        return denominator == 0 ? 0 : numerator / denominator;
    }

    /** Group samples into hourly buckets, keeping the bucket keys sorted. */
    static Map<LocalDateTime, List<Sample>> byHour(List<Sample> samples) {
        Map<LocalDateTime, List<Sample>> buckets = new TreeMap<>();
        for (Sample sample : samples) {
            LocalDateTime bucket = sample.at().truncatedTo(ChronoUnit.HOURS);
            buckets.computeIfAbsent(bucket, key -> new ArrayList<>()).add(sample);
        }
        return buckets;
    }

    public static void main(String[] args) {
        // ---- moving average ---------------------------------------------------
        List<Double> ramp = List.of(1.0, 2.0, 3.0, 4.0, 5.0);
        check(movingAverage(ramp, 3).equals(List.of(2.0, 3.0, 4.0)), "a three point average of a ramp");
        check(movingAverage(ramp, 1).equals(ramp), "a window of one changes nothing");
        check(movingAverage(ramp, 5).equals(List.of(3.0)), "one complete window");
        check(movingAverage(ramp, 6).isEmpty(), "a window longer than the data yields nothing");
        check(movingAverage(List.of(), 3).isEmpty(), "no data, no averages");
        try {
            movingAverage(ramp, 0);
            throw new AssertionError("a zero window should fail");
        } catch (IllegalArgumentException expected) {
            System.out.println("bad window  : " + expected.getMessage());
        }
        System.out.println("moving avg  : " + movingAverage(ramp, 3));

        // ---- exponential smoothing --------------------------------------------
        List<Double> ema = exponentialMovingAverage(ramp, 0.5);
        check(ema.get(0) == 1.0, "the first value seeds the series");
        check(ema.get(1) == 1.5, "then 0.5 * 2 + 0.5 * 1");
        check(Math.abs(ema.get(4) - 4.0625) < 1e-9, "and it converges towards the ramp");
        check(exponentialMovingAverage(List.of(), 0.5).isEmpty(), "no data, no smoothing");
        try {
            exponentialMovingAverage(ramp, 0.0);
            throw new AssertionError("alpha must be positive");
        } catch (IllegalArgumentException expected) {
            System.out.println("bad alpha   : " + expected.getMessage());
        }
        System.out.println("ema alpha .5: " + ema);

        // ---- anomalies ---------------------------------------------------------
        List<Double> readings = List.of(10.0, 10.0, 10.0, 10.0, 100.0);
        check(Math.abs(mean(readings) - 28.0) < 1e-9, "the mean is 28");
        check(Math.abs(standardDeviation(readings) - 36.0) < 1e-9, "the deviation is 36");
        check(Math.abs(zScore(readings, 4) - 2.0) < 1e-9, "so the spike has z = 2");
        check(anomalies(readings, 1.5).equals(List.of(4)), "the spike is the only anomaly at 1.5");
        check(anomalies(readings, 2.5).isEmpty(), "a stricter threshold flags nothing");
        check(anomalies(List.of(5.0, 5.0, 5.0), 1.0).isEmpty(),
                "when nothing varies, nothing can be anomalous");
        System.out.println("anomalies   : index " + anomalies(readings, 1.5)
                + " with z = " + zScore(readings, 4));

        // ---- trend -------------------------------------------------------------
        check(Math.abs(slope(ramp) - 1.0) < 1e-9, "a clean ramp rises by one per step");
        check(slope(List.of(7.0, 7.0, 7.0)) == 0.0, "a flat line has no slope");
        check(slope(List.of(9.0)) == 0.0, "one point cannot define a trend");
        check(Math.abs(slope(List.of(5.0, 3.0, 1.0)) + 2.0) < 1e-9,
                "and a falling line is negative: it drops two per step");
        System.out.printf("slope       : %.3f on the ramp, %.3f on the flat line%n",
                slope(ramp), slope(List.of(7.0, 7.0, 7.0)));

        // ---- bucketing ---------------------------------------------------------
        List<Sample> samples = List.of(
                new Sample(LocalDateTime.of(2026, 1, 5, 9, 5), 12.0),
                new Sample(LocalDateTime.of(2026, 1, 5, 9, 45), 18.0),
                new Sample(LocalDateTime.of(2026, 1, 5, 10, 15), 30.0),
                new Sample(LocalDateTime.of(2026, 1, 5, 10, 50), 10.0),
                new Sample(LocalDateTime.of(2026, 1, 5, 12, 0), 5.0));

        Map<LocalDateTime, List<Sample>> buckets = byHour(samples);
        check(buckets.size() == 3, "three hourly buckets");
        check(buckets.keySet().iterator().next().equals(LocalDateTime.of(2026, 1, 5, 9, 0)),
                "the keys are truncated to the hour and sorted");
        check(buckets.get(LocalDateTime.of(2026, 1, 5, 9, 0)).size() == 2, "two samples at nine");
        check(buckets.get(LocalDateTime.of(2026, 1, 5, 11, 0)) == null, "the empty hour has no bucket");

        for (Map.Entry<LocalDateTime, List<Sample>> bucket : buckets.entrySet()) {
            List<Double> values = bucket.getValue().stream().map(Sample::value).toList();
            System.out.printf("bucket %s : count=%d mean=%.2f%n",
                    bucket.getKey(), values.size(), mean(values));
        }

        // a rolling average over the bucketed means, to smooth the buckets
        List<Double> bucketMeans = buckets.values().stream()
                .map(bucket -> mean(bucket.stream().map(Sample::value).toList()))
                .toList();
        check(bucketMeans.size() == 3, "one mean per bucket");
        check(Math.abs(movingAverage(bucketMeans, 2).get(0) - (15.0 + 20.0) / 2) < 1e-9,
                "a rolling average over bucket means");
        System.out.println("bucket means: " + bucketMeans);
        System.out.println("All checks passed.");
    }
}
