public class TestTomasulo {
    public static void main(String[] args) {
        System.out.println("=== Tomasulo Simulator Test ===\n");
        
        SimulatorEngine engine = new SimulatorEngine();
        
        Register f2 = engine.getRegister("F2");
        Register f4 = engine.getRegister("F4");
        Register f6 = engine.getRegister("F6");
        
        f2.value = 5.0;
        f4.value = 3.0;
        
        System.out.println("Initial Register Values:");
        System.out.println("F2 = " + f2.value);
        System.out.println("F4 = " + f4.value);
        System.out.println();
        
        System.out.println("--- Test: ADD.D F6, F2, F4 (5.0 + 3.0 = 8.0) ---");
        Instruction inst1 = new Instruction(
            Instruction.OpType.ADD_D, 
            "F6", "F2", "F4", 0
        );
        
        boolean issued = engine.issueInstruction(inst1);
        System.out.println("Instruction issued: " + issued);
        System.out.println();
        
        for (int i = 1; i <= 5; i++) {
            System.out.println("\n========== Cycle " + i + " ==========");
            engine.nextCycle();
            engine.printStatus();
            
            if (i == 3) {
                System.out.println("\nF6 = " + f6.value + " (expected: 8.0)");
                System.out.println("F6.Qi = " + f6.Qi + " (should be null)");
            }
        }
        
        System.out.println("\n\nTEST COMPLETE!");
    }
}