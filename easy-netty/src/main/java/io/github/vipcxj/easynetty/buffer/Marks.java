package io.github.vipcxj.easynetty.buffer;

public class Marks {
    private int[] array;
    private int top;
    public Marks(int initCapacity) {
        this.array = new int[initCapacity];
        this.top = -1;
    }

    public void push(int value) {
        ensureSize(++this.top);
        array[top] = value;
    }

    public void pop() {
        if (top < 0) {
            throw new IllegalStateException("The stack is empty, the pop operation is not possible.");
        }
        --top;
    }

    public int top() {
        if (top < 0) {
            throw new IllegalStateException("The stack is empty, the top operation is not possible.");
        }
        return array[top];
    }

    public int min() {
        if (top < 0) {
            return -1;
        }
        int min = Integer.MAX_VALUE;
        for (int i = 0; i <= top; ++i) {
            int mark = array[i];
            if (mark < min) {
                min = mark;
            }
        }
        return min;
    }

    public void down(int offset) {
        if (top >= 0) {
            for (int i = 0; i <= top; ++i) {
                array[i] -= offset;
            }
        }
    }

    public void clean() {
        top = -1;
    }

    private void ensureSize(int size) {
        if (size >= array.length) {
            int[] newArray = new int[(int) (size * 1.5)];
            System.arraycopy(array, 0, newArray, 0, array.length);
            array = newArray;
        }
    }
}
