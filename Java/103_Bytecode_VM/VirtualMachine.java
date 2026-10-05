import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 103 - A stack based virtual machine: an assembler with labels, a small
 * instruction set, and an interpreter with an operand stack and local slots.
 *
 * Programs are built with the assembler rather than hand-written bytecode, so
 * the labels are resolved for you and the tests stay readable.
 *
 * Compile and run:
 *   javac VirtualMachine.java
 *   java VirtualMachine
 */
public class VirtualMachine {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    enum Op {
        PUSH,               // push the operand
        LOAD,               // push local[operand]
        STORE,              // local[operand] = pop()
        DUP,                // duplicate the top
        SWAP,               // swap the top two
        NEG,                // negate the top
        ADD, SUB, MUL, DIV, MOD,
        CMP_LT, CMP_LE, CMP_EQ, CMP_NE, CMP_GT, CMP_GE,   // push 1 or 0
        JUMP,               // unconditionally set the program counter
        JUMP_IF_ZERO,       // jump when the popped value is zero
        JUMP_IF_NONZERO,    // jump when the popped value is not zero
        PRINT,              // pop and record the output
        HALT
    }

    record Instruction(Op op, int operand) {
        @Override
        public String toString() {
            return takesOperand() ? op.name() + " " + operand : op.name();
        }

        private boolean takesOperand() {
            return op == Op.PUSH || op == Op.LOAD || op == Op.STORE
                    || op == Op.JUMP || op == Op.JUMP_IF_ZERO || op == Op.JUMP_IF_NONZERO;
        }
    }

    record Program(List<Instruction> code) {
        int size() {
            return code.size();
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < code.size(); i++) {
                out.append(String.format("%3d  %s%n", i, code.get(i)));
            }
            return out.toString();
        }
    }

    static final class Assembler {
        private final List<Instruction> code = new ArrayList<>();
        private final Map<String, Integer> labels = new LinkedHashMap<>();
        private final Map<Integer, String> unfinished = new LinkedHashMap<>();

        Assembler label(String name) {
            labels.put(name, code.size());
            return this;
        }

        Assembler op(Op op) {
            code.add(new Instruction(op, 0));
            return this;
        }

        Assembler op(Op op, int operand) {
            code.add(new Instruction(op, operand));
            return this;
        }

        Assembler jump(Op op, String label) {
            unfinished.put(code.size(), label);
            code.add(new Instruction(op, 0));
            return this;
        }

        Program build() {
            unfinished.forEach((index, label) -> {
                Integer target = labels.get(label);
                if (target == null) {
                    throw new IllegalArgumentException("no such label: " + label);
                }
                code.set(index, new Instruction(code.get(index).op(), target));
            });
            return new Program(List.copyOf(code));
        }
    }

    static class VmException extends RuntimeException {
        VmException(String message) {
            super(message);
        }
    }

    /** An operand stack, a few local slots, and a program counter. */
    static final class Vm {
        private static final int STACK_LIMIT = 256;

        private final int[] stack = new int[STACK_LIMIT];
        private final int[] locals = new int[64];
        private final List<Integer> output = new ArrayList<>();
        private int depth;

        private void push(int value) {
            if (depth == STACK_LIMIT) {
                throw new VmException("the operand stack overflowed");
            }
            stack[depth++] = value;
        }

        private int pop() {
            if (depth == 0) {
                throw new VmException("the operand stack underflowed");
            }
            return stack[--depth];
        }

        List<Integer> run(Program program, int stepLimit) {
            List<Instruction> code = program.code();
            int counter = 0;
            int steps = 0;

            while (counter < code.size()) {
                if (++steps > stepLimit) {
                    throw new VmException("the step limit of " + stepLimit + " was reached");
                }
                Instruction instruction = code.get(counter++);
                switch (instruction.op()) {
                    case PUSH -> push(instruction.operand());
                    case LOAD -> {
                        int slot = instruction.operand();
                        if (slot < 0 || slot >= locals.length) {
                            throw new VmException("no local slot " + slot);
                        }
                        push(locals[slot]);
                    }
                    case STORE -> {
                        int slot = instruction.operand();
                        if (slot < 0 || slot >= locals.length) {
                            throw new VmException("no local slot " + slot);
                        }
                        locals[slot] = pop();
                    }
                    case DUP -> {
                        int top = pop();
                        push(top);
                        push(top);
                    }
                    case SWAP -> {
                        int first = pop();
                        int second = pop();
                        push(first);
                        push(second);
                    }
                    case NEG -> push(-pop());
                    case ADD -> {
                        int right = pop();
                        int left = pop();
                        push(left + right);
                    }
                    case SUB -> {
                        int right = pop();
                        int left = pop();
                        push(left - right);
                    }
                    case MUL -> {
                        int right = pop();
                        int left = pop();
                        push(left * right);
                    }
                    case DIV -> {
                        int right = pop();
                        int left = pop();
                        if (right == 0) {
                            throw new VmException("division by zero at instruction " + (counter - 1));
                        }
                        push(left / right);
                    }
                    case MOD -> {
                        int right = pop();
                        int left = pop();
                        if (right == 0) {
                            throw new VmException("division by zero at instruction " + (counter - 1));
                        }
                        push(left % right);
                    }
                    // Pops right then left, so the written order matches the source.
                    case CMP_LT -> {
                        int right = pop();
                        int left = pop();
                        push(left < right ? 1 : 0);
                    }
                    case CMP_LE -> {
                        int right = pop();
                        int left = pop();
                        push(left <= right ? 1 : 0);
                    }
                    case CMP_GT -> {
                        int right = pop();
                        int left = pop();
                        push(left > right ? 1 : 0);
                    }
                    case CMP_GE -> {
                        int right = pop();
                        int left = pop();
                        push(left >= right ? 1 : 0);
                    }
                    case CMP_EQ -> {
                        int right = pop();
                        int left = pop();
                        push(left == right ? 1 : 0);
                    }
                    case CMP_NE -> {
                        int right = pop();
                        int left = pop();
                        push(left != right ? 1 : 0);
                    }
                    case JUMP -> counter = instruction.operand();
                    case JUMP_IF_ZERO -> {
                        if (pop() == 0) {
                            counter = instruction.operand();
                        }
                    }
                    case JUMP_IF_NONZERO -> {
                        if (pop() != 0) {
                            counter = instruction.operand();
                        }
                    }
                    case PRINT -> output.add(pop());
                    case HALT -> {
                        return List.copyOf(output);
                    }
                }
            }
            return List.copyOf(output);
        }

        int stackDepth() {
            return depth;
        }
    }

    /** Convenience: assemble and run, returning just the output. */
    static List<Integer> run(Program program) {
        return new Vm().run(program, 100_000);
    }

    public static void main(String[] args) {
        // ---- arithmetic -------------------------------------------------------
        Program arithmetic = new Assembler()
                .op(Op.PUSH, 2)
                .op(Op.PUSH, 3)
                .op(Op.ADD)                 // 5
                .op(Op.PUSH, 4)
                .op(Op.MUL)                 // 20
                .op(Op.PUSH, 6)
                .op(Op.PUSH, 2)
                .op(Op.DIV)                 // 3
                .op(Op.SUB)                 // 17
                .op(Op.PRINT)
                .op(Op.HALT)
                .build();
        check(run(arithmetic).equals(List.of(17)), "(2 + 3) * 4 - 6 / 2 is 17");
        System.out.println("arithmetic   : " + run(arithmetic));

        // ---- the stack instructions -------------------------------------------
        Program stackWork = new Assembler()
                .op(Op.PUSH, 7)
                .op(Op.DUP)
                .op(Op.ADD)                 // 14
                .op(Op.PUSH, 5)
                .op(Op.SWAP)                // 5 14
                .op(Op.SUB)                 // 5 - 14 = -9
                .op(Op.NEG)                 // 9
                .op(Op.PRINT)
                .op(Op.HALT)
                .build();
        check(run(stackWork).equals(List.of(9)), "dup, swap and negate combine as expected");
        System.out.println("stack ops    : " + run(stackWork));

        // ---- locals, a loop, and factorial ------------------------------------
        // n = 5; acc = 1; while n > 1 { acc *= n; n -= 1 }; print acc
        Program factorial = new Assembler()
                .op(Op.PUSH, 5)
                .op(Op.STORE, 0)            // n
                .op(Op.PUSH, 1)
                .op(Op.STORE, 1)            // acc
                .label("loop")
                .op(Op.LOAD, 0)
                .op(Op.PUSH, 1)
                .op(Op.CMP_LE)              // n <= 1 ?
                .jump(Op.JUMP_IF_NONZERO, "done")
                .op(Op.LOAD, 1)
                .op(Op.LOAD, 0)
                .op(Op.MUL)
                .op(Op.STORE, 1)            // acc *= n
                .op(Op.LOAD, 0)
                .op(Op.PUSH, 1)
                .op(Op.SUB)
                .op(Op.STORE, 0)            // n -= 1
                .jump(Op.JUMP, "loop")
                .label("done")
                .op(Op.LOAD, 1)
                .op(Op.PRINT)
                .op(Op.HALT)
                .build();
        check(run(factorial).equals(List.of(120)), "5! is 120");
        System.out.println("factorial(5) : " + run(factorial));

        // the same program with a different input
        Program factorialTen = new Assembler()
                .op(Op.PUSH, 10)
                .op(Op.STORE, 0)
                .op(Op.PUSH, 1)
                .op(Op.STORE, 1)
                .label("loop")
                .op(Op.LOAD, 0)
                .op(Op.PUSH, 1)
                .op(Op.CMP_LE)
                .jump(Op.JUMP_IF_NONZERO, "done")
                .op(Op.LOAD, 1)
                .op(Op.LOAD, 0)
                .op(Op.MUL)
                .op(Op.STORE, 1)
                .op(Op.LOAD, 0)
                .op(Op.PUSH, 1)
                .op(Op.SUB)
                .op(Op.STORE, 0)
                .jump(Op.JUMP, "loop")
                .label("done")
                .op(Op.LOAD, 1)
                .op(Op.PRINT)
                .op(Op.HALT)
                .build();
        check(run(factorialTen).equals(List.of(3_628_800)), "10! is 3628800");
        System.out.println("factorial(10): " + run(factorialTen));

        // ---- fibonacci, printing every value --------------------------------
        // a = 0; b = 1; repeat 10 times { print a; t = a + b; a = b; b = t }
        Program fibonacci = new Assembler()
                .op(Op.PUSH, 0)
                .op(Op.STORE, 0)            // a
                .op(Op.PUSH, 1)
                .op(Op.STORE, 1)            // b
                .op(Op.PUSH, 10)
                .op(Op.STORE, 2)            // count
                .label("loop")
                .op(Op.LOAD, 2)
                .jump(Op.JUMP_IF_ZERO, "done")
                .op(Op.LOAD, 0)
                .op(Op.PRINT)
                .op(Op.LOAD, 0)
                .op(Op.LOAD, 1)
                .op(Op.ADD)
                .op(Op.STORE, 3)            // t = a + b
                .op(Op.LOAD, 1)
                .op(Op.STORE, 0)            // a = b
                .op(Op.LOAD, 3)
                .op(Op.STORE, 1)            // b = t
                .op(Op.LOAD, 2)
                .op(Op.PUSH, 1)
                .op(Op.SUB)
                .op(Op.STORE, 2)            // count -= 1
                .jump(Op.JUMP, "loop")
                .label("done")
                .op(Op.HALT)
                .build();
        check(run(fibonacci).equals(List.of(0, 1, 1, 2, 3, 5, 8, 13, 21, 34)),
                "ten Fibonacci numbers");
        System.out.println("fibonacci    : " + run(fibonacci));

        // ---- comparisons ------------------------------------------------------
        check(run(new Assembler().op(Op.PUSH, 3).op(Op.PUSH, 5).op(Op.CMP_LT)
                .op(Op.PRINT).op(Op.HALT).build()).equals(List.of(1)), "3 < 5 is true");
        check(run(new Assembler().op(Op.PUSH, 5).op(Op.PUSH, 5).op(Op.CMP_LT)
                .op(Op.PRINT).op(Op.HALT).build()).equals(List.of(0)), "5 < 5 is false");
        check(run(new Assembler().op(Op.PUSH, 5).op(Op.PUSH, 5).op(Op.CMP_LE)
                .op(Op.PRINT).op(Op.HALT).build()).equals(List.of(1)), "5 <= 5 is true");
        check(run(new Assembler().op(Op.PUSH, 5).op(Op.PUSH, 5).op(Op.CMP_EQ)
                .op(Op.PRINT).op(Op.HALT).build()).equals(List.of(1)), "5 == 5 is true");
        check(run(new Assembler().op(Op.PUSH, 5).op(Op.PUSH, 6).op(Op.CMP_NE)
                .op(Op.PRINT).op(Op.HALT).build()).equals(List.of(1)), "5 != 6 is true");
        check(run(new Assembler().op(Op.PUSH, 6).op(Op.PUSH, 5).op(Op.CMP_GT)
                .op(Op.PRINT).op(Op.HALT).build()).equals(List.of(1)), "6 > 5 is true");
        check(run(new Assembler().op(Op.PUSH, 6).op(Op.PUSH, 6).op(Op.CMP_GE)
                .op(Op.PRINT).op(Op.HALT).build()).equals(List.of(1)), "6 >= 6 is true");
        System.out.println("comparisons  : all six push 1 or 0 correctly");

        // ---- modulo, negatives and no output ---------------------------------
        check(run(new Assembler().op(Op.PUSH, 17).op(Op.PUSH, 5).op(Op.MOD)
                .op(Op.PRINT).op(Op.HALT).build()).equals(List.of(2)), "17 mod 5 is 2");
        check(run(new Assembler().op(Op.PUSH, 3).op(Op.PUSH, 10).op(Op.SUB)
                .op(Op.PRINT).op(Op.HALT).build()).equals(List.of(-7)), "3 - 10 is -7");
        check(run(new Assembler().op(Op.PUSH, 1).op(Op.HALT).build()).isEmpty(),
                "a program that prints nothing returns nothing");

        // A completed program falls off the end just like HALT.
        check(run(new Assembler().op(Op.PUSH, 42).op(Op.PRINT).build()).equals(List.of(42)),
                "running past the last instruction stops the machine");
        System.out.println("misc         : modulo, negatives, fall-through and silence");

        // ---- the step limit stops runaway programs ---------------------------
        Program infinite = new Assembler()
                .label("spin")
                .jump(Op.JUMP, "spin")
                .build();
        try {
            new Vm().run(infinite, 1_000);
            throw new AssertionError("an endless loop should hit the step limit");
        } catch (VmException expected) {
            check(expected.getMessage().contains("step limit"), "the error names the limit");
            System.out.println("step limit   : " + expected.getMessage());
        }

        // ---- the failure modes ------------------------------------------------
        // A program with no HALT simply stops once it runs out of instructions.
        check(run(new Assembler().op(Op.PUSH, 1).op(Op.PRINT)
                .op(Op.PUSH, 2).op(Op.PRINT).build()).equals(List.of(1, 2)),
                "two values printed, then the machine stopped at the end of the program");

        try {
            run(new Assembler().op(Op.ADD).op(Op.HALT).build());
            throw new AssertionError("adding with an empty stack should fail");
        } catch (VmException expected) {
            System.out.println("underflow    : " + expected.getMessage());
        }

        try {
            run(new Assembler().op(Op.PUSH, 10).op(Op.PUSH, 0).op(Op.DIV)
                    .op(Op.HALT).build());
            throw new AssertionError("dividing by zero should fail");
        } catch (VmException expected) {
            System.out.println("divide by 0  : " + expected.getMessage());
        }

        try {
            run(new Assembler().op(Op.LOAD, 99).op(Op.HALT).build());
            throw new AssertionError("loading an unknown slot should fail");
        } catch (VmException expected) {
            System.out.println("bad local    : " + expected.getMessage());
        }

        // the stack is left in a sensible state when a run finishes
        Vm vm = new Vm();
        vm.run(new Assembler().op(Op.PUSH, 1).op(Op.PUSH, 2).op(Op.STORE, 0)
                .op(Op.HALT).build(), 1_000);
        check(vm.stackDepth() == 1, "one value was left on the stack");
        System.out.println("stack depth  : " + vm.stackDepth() + " after the run");

        // ---- a label that does not exist is caught at assembly time ----------
        try {
            new Assembler().jump(Op.JUMP, "nowhere").build();
            throw new AssertionError("an unknown label should fail while assembling");
        } catch (IllegalArgumentException expected) {
            System.out.println("bad label    : " + expected.getMessage());
        }

        // ---- the disassembly is informative ----------------------------------
        String listing = factorial.toString();
        check(listing.contains("JUMP_IF_NONZERO"), "the listing shows the jumps");
        check(listing.lines().count() == factorial.size(), "one line per instruction");
        check(factorial.size() == 20, "the factorial program is 20 instructions: " + factorial.size());
        System.out.println("listing      :\n" + listing);
        System.out.println("All checks passed.");
    }
}
