type Matrix = number[][];

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function shape(matrix: Matrix): [number, number] {
  return [matrix.length, matrix[0]?.length ?? 0];
}

function validate(matrix: Matrix, name: string): [number, number] {
  const [rows, columns] = shape(matrix);
  for (let row = 0; row < rows; row += 1) {
    if (matrix[row].length !== columns) {
      throw new Error(`${name} is ragged: row ${row} has ${matrix[row].length} values, expected ${columns}`);
    }
  }
  return [rows, columns];
}

function zeros(rows: number, columns: number): Matrix {
  return Array.from({ length: rows }, () => new Array<number>(columns).fill(0));
}

function identity(size: number): Matrix {
  const result = zeros(size, size);
  for (let index = 0; index < size; index += 1) {
    result[index][index] = 1;
  }
  return result;
}

function add(left: Matrix, right: Matrix): Matrix {
  const [leftRows, leftColumns] = validate(left, "left");
  const [rightRows, rightColumns] = validate(right, "right");
  check(leftRows === rightRows && leftColumns === rightColumns, "matrices must have the same shape");

  return left.map((row, rowIndex) => row.map((value, columnIndex) => value + right[rowIndex][columnIndex]));
}

function scale(matrix: Matrix, factor: number): Matrix {
  return matrix.map((row) => row.map((value) => value * factor));
}

function multiply(left: Matrix, right: Matrix): Matrix {
  const [leftRows, leftColumns] = validate(left, "left");
  const [rightRows, rightColumns] = validate(right, "right");
  check(leftColumns === rightRows, `cannot multiply ${leftRows}x${leftColumns} by ${rightRows}x${rightColumns}`);

  const result = zeros(leftRows, rightColumns);
  for (let row = 0; row < leftRows; row += 1) {
    for (let column = 0; column < rightColumns; column += 1) {
      let total = 0;
      for (let inner = 0; inner < leftColumns; inner += 1) {
        total += left[row][inner] * right[inner][column];
      }
      result[row][column] = total;
    }
  }
  return result;
}

function transpose(matrix: Matrix): Matrix {
  const [rows, columns] = validate(matrix, "matrix");
  const result = zeros(columns, rows);
  for (let row = 0; row < rows; row += 1) {
    for (let column = 0; column < columns; column += 1) {
      result[column][row] = matrix[row][column];
    }
  }
  return result;
}

function squareSize(matrix: Matrix): number {
  const [rows, columns] = validate(matrix, "matrix");
  check(rows === columns, `expected a square matrix, got ${rows}x${columns}`);
  return rows;
}

function trace(matrix: Matrix): number {
  const size = squareSize(matrix);
  let total = 0;
  for (let index = 0; index < size; index += 1) {
    total += matrix[index][index];
  }
  return total;
}

/** Gaussian elimination with partial pivoting. */
function determinant(matrix: Matrix): number {
  const size = squareSize(matrix);
  const work = matrix.map((row) => [...row]);
  let result = 1;

  for (let column = 0; column < size; column += 1) {
    let pivot = column;
    for (let row = column + 1; row < size; row += 1) {
      if (Math.abs(work[row][column]) > Math.abs(work[pivot][column])) {
        pivot = row;
      }
    }

    if (Math.abs(work[pivot][column]) < 1e-12) {
      return 0;
    }

    if (pivot !== column) {
      [work[pivot], work[column]] = [work[column], work[pivot]];
      result = -result;
    }

    const pivotValue = work[column][column];
    result *= pivotValue;

    for (let row = column + 1; row < size; row += 1) {
      const factor = work[row][column] / pivotValue;
      for (let inner = column; inner < size; inner += 1) {
        work[row][inner] -= factor * work[column][inner];
      }
    }
  }

  return result;
}

function inverse(matrix: Matrix): Matrix {
  const size = squareSize(matrix);
  const work = matrix.map((row, rowIndex) => [
    ...row,
    ...identity(size)[rowIndex],
  ]);

  for (let column = 0; column < size; column += 1) {
    let pivot = column;
    for (let row = column + 1; row < size; row += 1) {
      if (Math.abs(work[row][column]) > Math.abs(work[pivot][column])) {
        pivot = row;
      }
    }

    if (Math.abs(work[pivot][column]) < 1e-12) {
      throw new Error("matrix is singular and has no inverse");
    }

    [work[pivot], work[column]] = [work[column], work[pivot]];

    const pivotValue = work[column][column];
    for (let inner = 0; inner < size * 2; inner += 1) {
      work[column][inner] /= pivotValue;
    }

    for (let row = 0; row < size; row += 1) {
      if (row === column) {
        continue;
      }
      const factor = work[row][column];
      for (let inner = 0; inner < size * 2; inner += 1) {
        work[row][inner] -= factor * work[column][inner];
      }
    }
  }

  return work.map((row) => row.slice(size));
}

function approximatelyEqual(left: Matrix, right: Matrix, epsilon = 1e-9): boolean {
  const [leftRows, leftColumns] = validate(left, "left");
  const [rightRows, rightColumns] = validate(right, "right");
  if (leftRows !== rightRows || leftColumns !== rightColumns) {
    return false;
  }
  for (let row = 0; row < leftRows; row += 1) {
    for (let column = 0; column < leftColumns; column += 1) {
      if (Math.abs(left[row][column] - right[row][column]) > epsilon) {
        return false;
      }
    }
  }
  return true;
}

function format(matrix: Matrix): string {
  return matrix.map((row) => row.map((value) => value.toFixed(2).padStart(8)).join(" ")).join("\n");
}

/** Cofactor expansion, only used to cross-check the elimination version. */
function determinantRecursive(matrix: Matrix): number {
  const size = matrix.length;
  if (size === 1) {
    return matrix[0][0];
  }
  if (size === 2) {
    return matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];
  }

  let total = 0;
  for (let column = 0; column < size; column += 1) {
    const minor = matrix.slice(1).map((row) => row.filter((_, index) => index !== column));
    total += (column % 2 === 0 ? 1 : -1) * matrix[0][column] * determinantRecursive(minor);
  }
  return total;
}

function main(): void {
  console.log("--- basic algebra ---");
  const left: Matrix = [
    [1, 2, 3],
    [4, 5, 6],
  ];
  const right: Matrix = [
    [7, 8],
    [9, 10],
    [11, 12],
  ];

  const product = multiply(left, right);
  console.log("product:\n" + format(product));
  check(approximatelyEqual(product, [[58, 64], [139, 154]]), "3x2 times 2x3");

  const summed = add(left, left);
  console.log("sum:\n" + format(summed));
  check(approximatelyEqual(summed, [[2, 4, 6], [8, 10, 12]]), "addition is elementwise");
  check(approximatelyEqual(scale(left, 2), summed), "scaling by two equals adding to itself");

  console.log("\n--- transposition ---");
  const transposed = transpose(left);
  console.log("transpose:\n" + format(transposed));
  check(approximatelyEqual(transposed, [[1, 4], [2, 5], [3, 6]]), "rows become columns");
  check(approximatelyEqual(transpose(transposed), left), "transposing twice is the identity");
  check(approximatelyEqual(multiply(left, identity(3)), left), "multiplying by I does nothing");
  check(approximatelyEqual(trace([[1, 2], [3, 4]]), 5), "trace sums the diagonal");

  console.log("\n--- shape errors ---");
  const errors: Array<[string, () => void]> = [
    ["ragged matrix", () => transpose([[1, 2], [3]])],
    ["add mismatched", () => add([[1]], [[1, 2]])],
    ["multiply mismatched", () => multiply([[1, 2]], [[1, 2]])],
    ["trace non-square", () => trace([[1, 2]])],
    ["determinant non-square", () => determinant([[1, 2, 3], [4, 5, 6]])],
  ];
  for (const [label, attempt] of errors) {
    try {
      attempt();
      throw new Error(`${label} should have failed`);
    } catch (error) {
      console.log(`  ${label}: ${(error as Error).message}`);
    }
  }

  console.log("\n--- determinants ---");
  check(Math.abs(determinant([[1, 2], [3, 4]]) - -2) < 1e-9, "2x2 determinant");
  check(Math.abs(determinant([[6, 1, 1], [4, -2, 5], [2, 8, 7]]) - -306) < 1e-6, "3x3 determinant");
  check(Math.abs(determinant([[2, 0, 0], [0, 3, 0], [0, 0, 4]]) - 24) < 1e-9, "diagonal determinant");
  check(Math.abs(determinant([[1, 2], [2, 4]])) < 1e-12, "a dependent row gives zero");
  check(Math.abs(determinant(identity(5)) - 1) < 1e-12, "identity has determinant one");
  console.log("2x2, 3x3, diagonal, singular, and identity determinants match");

  console.log("\n--- inverses ---");
  const invertible: Matrix = [
    [4, 7],
    [2, 6],
  ];
  const inverted = inverse(invertible);
  console.log("inverse of [[4, 7], [2, 6]]:\n" + format(inverted));
  check(
    approximatelyEqual(inverted, [
      [0.6, -0.7],
      [-0.2, 0.4],
    ]),
    "2x2 inverse",
  );
  check(approximatelyEqual(multiply(invertible, inverted), identity(2)), "A times A inverse is I");

  console.log("\n--- random matrices against cofactor expansion ---");
  let state = 987654321;
  const next = (): number => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state;
  };

  let checked = 0;
  for (let trial = 0; trial < 300; trial += 1) {
    const size = 1 + (next() % 4);
    const matrix: Matrix = Array.from({ length: size }, () =>
      Array.from({ length: size }, () => (next() % 11) - 5),
    );
    const elimination = determinant(matrix);
    const cofactor = determinantRecursive(matrix);
    check(Math.abs(elimination - cofactor) < 1e-6, `determinant mismatch ${size}x${size}`);
    checked += 1;
  }
  console.log(`${checked} random matrices agree with cofactor expansion`);

  console.log("\n--- random inverses ---");
  let invertedCount = 0;
  for (let trial = 0; trial < 200; trial += 1) {
    const size = 2 + (next() % 3);
    const matrix: Matrix = Array.from({ length: size }, () =>
      Array.from({ length: size }, () => (next() % 9) - 4),
    );
    if (Math.abs(determinant(matrix)) < 1e-6) {
      continue;
    }
    const result = inverse(matrix);
    check(approximatelyEqual(multiply(matrix, result), identity(size), 1e-6), `inverse failed for size ${size}`);
    check(approximatelyEqual(multiply(result, matrix), identity(size), 1e-6), `left inverse failed for size ${size}`);
    invertedCount += 1;
  }
  console.log(`${invertedCount} random invertible matrices multiplied back to the identity`);

  console.log("\n--- singular matrices are rejected ---");
  const singular: Matrix[] = [
    [[1, 2], [2, 4]],
    [[1, 2, 3], [4, 5, 6], [7, 8, 9]],
  ];
  for (const matrix of singular) {
    let threw = false;
    try {
      inverse(matrix);
    } catch (error) {
      threw = true;
      void error;
    }
    check(threw, "singular matrices have no inverse");
  }
  console.log("both singular examples threw");

  console.log("\nAll checks passed.");
}

main();
