// package simulation;

// import components.*;
// import model.*;
// import java.util.List;

// public class SimulatorEngine {
//     // Hardware Components
//     private MemoryUnit memory;      
//     private RegisterFile registerFile;
//     private List<ReservationStation> addStations;
//     private List<ReservationStation> mulStations;
//     private int currentCycle = 0;

//     public SimulatorEngine() {
//         // Initialize register file with a reasonable default size
//         this.registerFile = new RegisterFile(32, 32);
//         // Initialize a couple of demo reservation stations so execution flow can be exercised
//         this.addStations = new java.util.ArrayList<>();
//         this.mulStations = new java.util.ArrayList<>();
//         this.addStations.add(new ReservationStation("ADD1"));
//         this.addStations.add(new ReservationStation("ADD2"));
//         this.mulStations.add(new ReservationStation("MUL1"));
//         this.mulStations.add(new ReservationStation("MUL2"));
//     }

//     public void nextCycle() {
//         currentCycle++;

//         // 1. Issue Stage (Member 2's logic)
//         // Check Instruction Queue -> Find free station -> Move instruction

//         // 2. Execute Stage (Member 3 & 4's logic)
//         // Tick timers on all busy stations. If timer reaches 0 compute result and request CDB.
//         for (ReservationStation rs : addStations) {
//             if (rs.busy && rs.timeLeft > 0) {
//                 rs.timeLeft--;
//                 if (rs.timeLeft == 0) {
//                     // Simple execution: do arithmetic based on op
//                     double res = 0.0;
//                     if (rs.op != null) {
//                         String o = rs.op.toUpperCase();
//                         if (o.contains("ADD")) res = rs.Vj + rs.Vk;
//                         else if (o.contains("SUB")) res = rs.Vj - rs.Vk;
//                         else if (o.contains("MUL")) res = rs.Vj * rs.Vk;
//                         else if (o.contains("DIV")) res = rs.Vj / rs.Vk;
//                         else res = rs.Vj;
//                     }
//                     rs.markExecutionComplete(res);
//                 }
//             }
//         }
//         for (ReservationStation rs : mulStations) {
//             if (rs.busy && rs.timeLeft > 0) {
//                 rs.timeLeft--;
//                 if (rs.timeLeft == 0) {
//                     double res = 0.0;
//                     if (rs.op != null) {
//                         String o = rs.op.toUpperCase();
//                         if (o.contains("MUL")) res = rs.Vj * rs.Vk;
//                         else if (o.contains("DIV")) res = rs.Vj / rs.Vk;
//                         else if (o.contains("ADD")) res = rs.Vj + rs.Vk;
//                         else res = rs.Vj;
//                     }
//                     rs.markExecutionComplete(res);
//                 }
//             }
//         }

//         // 3. Write Result Stage (Member 6's logic)
//         // Resolve exactly one pending publish request via the Common Data Bus arbitration.
//         try {
//             CommonDataBus.getInstance().resolveNextPublish();
//         } catch (Exception e) {
//             // ignore if CDB not available
//         }

//         // 4. Update Tables (Member 1)

//         // Reset the Common Data Bus at the end of the cycle so it can accept a new publisher next cycle
//         try {
//             CommonDataBus.getInstance().resetBus();
//         } catch (Exception e) {
//             // ignore if CDB not yet initialized
//         }
//     }
// }