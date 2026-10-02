import java.util.Arrays;

/**
 * 85 - Matrix algebra: multiplication, transpose, determinant, solving a linear
 * system and inverting a matrix, all with Gaussian elimination and partial
 * pivoting to keep the arithmetic stable.
 *
 * Compile and run:
 *   javac MatrixAlgebra.java
 *   java MatrixAlgebra
 */
public class MatrixAlgebra {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final double EPSILON = 1e-9;

    static double[][] copy(double[][] source) {
        double[][] result = new double[source.length][];
        for (int row = 0; row < source.length; row++) {
            result[row] = source[row].clone();
        }
        return result;
    }

    static double[][] identity(int size) {
        double[][] result = new double[size][size];
        for (int i = 0; i < size; i++) {
            result[i][i] = 1;
        }
        return result;
    }

    static double[][] multiply(double[][] left, double[][] right) {
        if (left[0].length != right.length) {
            throw new IllegalArgumentException("cannot multiply "
                    + shape(left) + " by " + shape(right));
        }
        double[][] result = new double[left.length][right[0].length];
        for (int row = 0; row < left.length; row++) {
            for (int column = 0; column < right[0].length; column++) {
                double total = 0;
                for (int inner = 0; inner < left[0].length; inner++) {
                    total += left[row][inner] * right[inner][column];
                }
                result[row][column] = total;
            }
        }
        return result;
    }

    static double[] multiply(double[][] matrix, double[] vector) {
        if (matrix[0].length != vector.length) {
            throw new IllegalArgumentException("the vector does not match the matrix width");
        }
        double[] result = new double[matrix.length];
        for (int row = 0; row < matrix.length; row++) {
            double total = 0;
            for (int column = 0; column < vector.length; column++) {
                total += matrix[row][column] * vector[column];
            }
            result[row] = total;
        }
        return result;
    }

    static double[][] transpose(double[][] source) {
        double[][] result = new double[source[0].length][source.length];
        for (int row = 0; row < source.length; row++) {
            for (int column = 0; column < source[0].length; column++) {
                result[column][row] = source[row][column];
            }
        }
        return result;
    }

    static boolean almostEquals(double[][] left, double[][] right) {
        if (left.length != right.length || left[0].length != right[0].length) {
            return false;
        }
        for (int row = 0; row < left.length; row++) {
            for (int column = 0; column < left[0].length; column++) {
                if (Math.abs(left[row][column] - right[row][column]) > EPSILON) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean almostEquals(double[] left, double[] right) {
        if (left.length != right.length) {
            return false;
        }
        for (int i = 0; i < left.length; i++) {
            if (Math.abs(left[i] - right[i]) > EPSILON) {
                return false;
            }
        }
        return true;
    }

    static String shape(double[][] matrix) {
        return matrix.length + "x" + matrix[0].length;
    }

    /** Swap the row with the largest remaining pivot into place. */
    private static int pivotRow(double[][] matrix, int column) {
        int best = column;
        for (int row = column + 1; row < matrix.length; row++) {
            if (Math.abs(matrix[row][column]) > Math.abs(matrix[best][column])) {
                best = row;
            }
        }
        return best;
    }

    private static void swapRows(double[][] matrix, int first, int second) {
        double[] temporary = matrix[first];
        matrix[first] = matrix[second];
        matrix[second] = temporary;
    }

    /**
     * Determinant by elimination. Zero appears exactly when a pivot vanishes,
     * which is why the singular check uses an epsilon rather than == 0.
     */
    static double determinant(double[][] source) {
        double[][] work = copy(source);
        int size = work.length;
        double determinant = 1;

        for (int column = 0; column < size; column++) {
            int pivot = pivotRow(work, column);
            if (Math.abs(work[pivot][column]) < EPSILON) {
                return 0;                      // a whole column is zero: singular
            }
            if (pivot != column) {
                swapRows(work, pivot, column);
                determinant = -determinant;    // each row swap flips the sign
            }
            determinant *= work[column][column];
            for (int row = column + 1; row < size; row++) {
                double factor = work[row][column] / work[column][column];
                for (int k = column; k < size; k++) {
                    work[row][k] -= factor * work[column][k];
                }
            }
        }
        return determinant;
    }

    /** Solve Ax = b by forward elimination and back substitution. */
    static double[] solve(double[][] source, double[] rhs) {
        int size = source.length;
        double[][] work = copy(source);
        double[] result = rhs.clone();

        for (int column = 0; column < size; column++) {
            int pivot = pivotRow(work, column);
            if (Math.abs(work[pivot][column]) < EPSILON) {
                throw new ArithmeticException("the matrix is singular, so there is no unique solution");
            }
            if (pivot != column) {
                swapRows(work, pivot, column);
                double temporary = result[pivot];
                result[pivot] = result[column];
                result[column] = temporary;
            }
            for (int row = column + 1; row < size; row++) {
                double factor = work[row][column] / work[column][column];
                for (int k = column; k < size; k++) {
                    work[row][k] -= factor * work[column][k];
                }
                result[row] -= factor * result[column];
            }
        }

        for (int row = size - 1; row >= 0; row--) {
            double total = result[row];
            for (int column = row + 1; column < size; column++) {
                total -= work[row][column] * result[column];
            }
            result[row] = total / work[row][row];
        }
        return result;
    }

    /** Gauss-Jordan on [A | I] leaves [I | A^-1]. */
    static double[][] inverse(double[][] source) {
        int size = source.length;
        double[][] work = new double[size][size * 2];
        for (int row = 0; row < size; row++) {
            System.arraycopy(source[row], 0, work[row], 0, size);
            work[row][size + row] = 1;
        }

        for (int column = 0; column < size; column++) {
            int pivot = pivotRow(work, column);
            if (Math.abs(work[pivot][column]) < EPSILON) {
                throw new ArithmeticException("the matrix is singular, so it cannot be inverted");
            }
            swapRows(work, pivot, column);
            double scale = work[column][column];
            for (int k = 0; k < size * 2; k++) {
                work[column][k] /= scale;
            }
            for (int row = 0; row < size; row++) {
                if (row == column) {
                    continue;
                }
                double factor = work[row][column];
                for (int k = 0; k < size * 2; k++) {
                    work[row][k] -= factor * work[column][k];
                }
            }
        }

        double[][] result = new double[size][size];
        for (int row = 0; row < size; row++) {
            System.arraycopy(work[row], size, result[row], 0, size);
        }
        return result;
    }

    public static void main(String[] args) {
        double[][] a = {{4, 7}, {2, 6}};
        double[][] b = {{1, 2, 3}, {4, 5, 6}, {7, 8, 10}};

        // ---- shapes and multiplication ---------------------------------------
        check(shape(a).equals("2x2") && shape(b).equals("3x3"), "the shapes are reported");
        check(almostEquals(multiply(a, identity(2)), a), "multiplying by the identity changes nothing");
        check(almostEquals(multiply(identity(3), b), b), "on either side");
        double[][] product = multiply(a, transpose(a));
        check(product[0][0] == 4 * 4 + 7 * 7 && product[0][1] == 4 * 2 + 7 * 6,
                "A times A transpose is computed element by element");
        check(product[0][1] == product[1][0], "and it is symmetric");
        try {
            multiply(a, b);
            throw new AssertionError("2x2 by 3x3 should not multiply");
        } catch (IllegalArgumentException expected) {
            System.out.println("bad shape    : " + expected.getMessage());
        }

        // ---- transpose --------------------------------------------------------
        double[][] transposed = transpose(a);
        check(transposed[0][1] == a[1][0] && transposed[1][0] == a[0][1], "transpose swaps the indices");
        check(almostEquals(transpose(transpose(a)), a), "doing it twice is the identity");
        // the classic identity: (AB)^T = B^T A^T, over a 2x3 and a 3x2
        double[][] c = {{1, 2, 3}, {4, 5, 6}};
        double[][] d = {{7, 8}, {9, 10}, {11, 12}};
        check(almostEquals(transpose(multiply(c, d)), multiply(transpose(d), transpose(c))),
                "(AB) transpose equals B transpose times A transpose");
        check(shape(multiply(c, d)).equals("2x2"), "the 2x3 by 3x2 product is 2x2");

        // ---- determinants -----------------------------------------------------
        check(Math.abs(determinant(a) - 10.0) < EPSILON, "det of the 2x2 is 4*6 - 7*2 = 10");
        check(Math.abs(determinant(b) - (-3.0)) < EPSILON, "det of the 3x3 is -3");
        check(Math.abs(determinant(identity(4)) - 1.0) < EPSILON, "the identity has determinant 1");
        check(Math.abs(determinant(new double[][] {{5}}) - 5.0) < EPSILON, "a 1x1 is just its value");
        check(Math.abs(determinant(transpose(b)) - determinant(b)) < EPSILON,
                "transposing does not change the determinant");
        double[][] triangular = {{2, 9, 9}, {0, 3, 9}, {0, 0, 4}};
        check(Math.abs(determinant(triangular) - 24.0) < EPSILON,
                "a triangular matrix is the product of its diagonal");
        double[][] singular = {{1, 2}, {2, 4}};
        check(determinant(singular) == 0.0, "a singular matrix has determinant zero");
        System.out.printf("determinants : %.1f, %.1f, %.1f%n",
                determinant(a), determinant(b), determinant(triangular));

        // ---- solving a system -------------------------------------------------
        double[] rhs = {1, 0};
        double[] solution = solve(a, rhs);
        check(almostEquals(solution, new double[] {0.6, -0.2}), "the solution is 0.6 and -0.2");
        check(almostEquals(multiply(a, solution), rhs), "and it satisfies A x = b");
        double[] general = solve(b, new double[] {1, 2, 3});
        check(almostEquals(multiply(b, general), new double[] {1, 2, 3}), "the 3x3 system checks out");
        try {
            solve(singular, rhs);
            throw new AssertionError("a singular system has no unique solution");
        } catch (ArithmeticException expected) {
            System.out.println("singular     : " + expected.getMessage());
        }

        // ---- inverting --------------------------------------------------------
        double[][] inverseA = inverse(a);
        check(almostEquals(inverseA, new double[][] {{0.6, -0.7}, {-0.2, 0.4}}),
                "the inverse is the expected matrix");
        check(almostEquals(multiply(a, inverseA), identity(2)), "A times A inverse is the identity");
        check(almostEquals(multiply(inverseA, a), identity(2)), "in both orders");
        check(Math.abs(determinant(inverseA) - 0.1) < EPSILON, "det of the inverse is the reciprocal");
        check(almostEquals(multiply(inverseA, rhs), solution), "the inverse gives the same answer as solve");

        double[][] inverseB = inverse(b);
        check(almostEquals(multiply(b, inverseB), identity(3)), "the 3x3 inverts correctly too");
        check(almostEquals(inverse(inverseB), b), "inverting twice returns the original");
        try {
            inverse(singular);
            throw new AssertionError("a singular matrix cannot be inverted");
        } catch (ArithmeticException expected) {
            System.out.println("not invertible: " + expected.getMessage());
        }
        System.out.println("All checks passed.");
    }
}
