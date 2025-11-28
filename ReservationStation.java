// package components;

// import simulation.CommonDataBus;

// public class ReservationStation implements CommonDataBus.Listener {
//     public String name; // e.g., "ADD1", "MUL2"
//     public boolean busy;
//     public String op;   // Operation being performed
//     public double Vj;   // Value of operand j
//     public double Vk;   // Value of operand k
//     public String Qj;   // Name of RS producing Vj
//     public String Qk;   // Name of RS producing Vk
//     public int timeLeft; // Cycles remaining for execution
//     public double result; // The calculated result
//     // When execution finishes but CDB was busy, we keep pending publish info
//     public boolean pendingPublish = false;
//     public double pendingValue = 0.0;

//     public ReservationStation(String name) {
//         this.name = name;
//         this.busy = false;
//         this.op = null;
//         this.Vj = 0.0;
//         this.Vk = 0.0;
//         this.Qj = null;
//         this.Qk = null;
//         this.timeLeft = 0;
//         this.result = 0.0;
//         // Register to CDB so this station automatically receives broadcasts
//         try {
//             CommonDataBus.getInstance().registerListener(this);
//         } catch (Exception e) {
//             // ignore registration errors during startup ordering
//         }
//     }

//     public void listenToCDB(String tag, double value) {
//         if (tag == null) return;
//         if (this.Qj != null && this.Qj.equals(tag)) {
//             this.Vj = value;
//             this.Qj = null;
//         }
//         if (this.Qk != null && this.Qk.equals(tag)) {
//             this.Vk = value;
//             this.Qk = null;
//         }
//     }

//     @Override
//     public void onCDBBroadcast(String tag, double value) {
//         // If this station is the publisher, clear its pending flag and mark write-back done
//         if (tag != null && tag.equals(this.name)) {
//             this.pendingPublish = false;
//             this.busy = false;
//             this.op = null;
//         }
//         listenToCDB(tag, value);
//     }

//     // Called when execution finishes to attempt publishing result to CDB
//     public void markExecutionComplete(double value) {
//         this.result = value;
//         this.pendingValue = value;
//         this.pendingPublish = true;
//         try {
//             simulation.CommonDataBus.getInstance().requestPublish(this.name, value);
//         } catch (Exception e) {
//             // ignore; engine will retry resolving pending publishes
//         }
//     }

//     // Retry a pending publish (called by the engine during writeback stage)
//     public void tryPublishPending() {
//         if (!this.pendingPublish) return;
//         try {
//             boolean ok = simulation.CommonDataBus.getInstance().publish(this.name, this.pendingValue);
//             if (ok) {
//                 this.pendingPublish = false;
//                 this.busy = false;
//                 this.op = null;
//             }
//         } catch (Exception e) {
//             // ignore and retry later
//         }
//     }
// }