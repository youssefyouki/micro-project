package components;
import java.util.LinkedList;
import java.util.Queue;

public class InstructionQueue {
    private Queue<Instruction> queue = new LinkedList<>();

    public InstructionQueue() {
        // TODO: Later you will parse the text file to fill this.
        // For now, let's add ONE dummy instruction so you see something happen.
        queue.add(new Instruction("ADD", "F1", "F2", "F3"));
    }

    public boolean hasNext() { return !queue.isEmpty(); }
    public Instruction peek() { return queue.peek(); }
    public void pop() { queue.poll(); }
}