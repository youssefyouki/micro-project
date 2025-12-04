package simulation;

import components.*;
import java.util.ArrayList;
import java.util.List;

public class SimulatorEngine {
    
    // --- Hardware Components ---
    private MemoryUnit memory;
    private RegisterFile floatRegFile;
    private RegisterFile intRegFile;
    
    // Reservation Stations
    private List<ReservationStation> addStations;
    private List<ReservationStation> mulStations;
    private List<ReservationStation> loadBuffers;
    private List<ReservationStation> storeBuffers;
    
    // Instruction Queue
    private InstructionQueue instructionQueue; // You need to ask Member 2 for this
    
    // System State
    private int currentCycle = 0;
    private int addLatency;
    private int mulLatency;
    private int divLatency;
    private int loadLatency;

    /**
     * Constructor: Initializes the hardware with user configurations.
     */
    public SimulatorEngine(int addLat, int mulLat, int divLat, int loadLat, int cacheSize, int blockSize) {
        this.addLatency = addLat;
        this.mulLatency = mulLat;
        this.divLatency = divLat;
        this.loadLatency = loadLat;
        
        // Initialize Components
        int numBlocks = cacheSize / blockSize;
        this.memory = new MemoryUnit(cacheSize, blockSize, numBlocks);
        this.floatRegFile = new RegisterFile(32, "F");
        this.intRegFile = new RegisterFile(32, "R");
        
        // Create Stations (Sizes could also be inputs if you want)
        this.addStations = createStations(3, "ADD");
        this.mulStations = createStations(2, "MUL");
        this.loadBuffers = createStations(3, "LOAD");
        this.storeBuffers = createStations(3, "STORE");
        
        this.instructionQueue = new InstructionQueue();
    }

    /**
     * Helper to create a list of stations
     */
    private List<ReservationStation> createStations(int count, String type) {
        List<ReservationStation> stations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            stations.add(new ReservationStation(type + (i + 1), type));
        }
        return stations;
    }

    /**
     * THE HEARTBEAT: Moves the processor forward one cycle.
     */
    public void nextCycle() {
        currentCycle++;

        // ---------------------------------------------------------
        // 1. EXECUTE STAGE (Count down timers)
        // ---------------------------------------------------------
        // We do this first so that an instruction finishing this cycle 
        // is ready for the Write-Back phase below.
        executeStationList(addStations);
        executeStationList(mulStations);
        executeStationList(loadBuffers); 
        // Note: Stores are special, usually handled at Commit, but for this project 
        // check with your team if they write back to memory here.

        // ---------------------------------------------------------
        // 2. WRITE BACK STAGE (Bus Arbitration)
        // ---------------------------------------------------------
        // Requirement: "When two instructions wish to publish their result... handle that"
        ReservationStation winner = null;

        // Collect all candidates who finished execution THIS cycle (timeLeft == 0)
        List<ReservationStation> readyStations = new ArrayList<>();
        readyStations.addAll(getFinishedStations(addStations));
        readyStations.addAll(getFinishedStations(mulStations));
        readyStations.addAll(getFinishedStations(loadBuffers));

        // ARBITRATION LOGIC: Pick the "Winner"
        // Policy: Oldest instruction (or random/first found) goes first. 
        // Let's use 'first found' for simplicity, but you can add an 'issueCycle' field to sort by.
        if (!readyStations.isEmpty()) {
            winner = readyStations.get(0); // The chosen one
            
            // Broadcast the result (Common Data Bus)
            double result = winner.getResult();
            String tag = winner.getTag();
            
            // Update all components waiting for this tag
            broadcastToStations(addStations, tag, result);
            broadcastToStations(mulStations, tag, result);
            broadcastToStations(storeBuffers, tag, result);
            
            floatRegFile.updateWaitingRegisters(tag, result);
            intRegFile.updateWaitingRegisters(tag, result);
            
            // Clear the winning station
            winner.clear();
        }

        // ---------------------------------------------------------
        // 3. ISSUE STAGE
        // ---------------------------------------------------------
        // If queue has instruction, try to find a free station
        if (instructionQueue.hasNext()) {
            Instruction instr = instructionQueue.peek();
            
            // Logic to find the correct station list based on instruction type
            List<ReservationStation> targetList = getTargetList(instr.getOpcode());
            
            // Try to find a free spot
            for (ReservationStation rs : targetList) {
                if (!rs.isBusy()) {
                    // ISSUE IT!
                    instructionQueue.pop(); // Remove from queue
                    
                    // You need to calculate Latency based on type
                    int latency = getLatencyForOp(instr.getOpcode());
                    
                    // Setup the station (read registers, set busy, etc.)
                    // This logic usually belongs in the RS class, e.g., rs.issue(instr, regFile);
                    rs.issue(instr, floatRegFile, intRegFile, latency);
                    break;
                }
            }
        }
    }

    // --- Helper Methods ---

    private void executeStationList(List<ReservationStation> stations) {
        for (ReservationStation rs : stations) {
            // Only count down if busy AND operands are ready (Qj and Qk are null)
            if (rs.isBusy() && rs.isReadyToExecute()) {
                rs.decrementTimer();
            }
        }
    }

    private List<ReservationStation> getFinishedStations(List<ReservationStation> stations) {
        List<ReservationStation> finished = new ArrayList<>();
        for (ReservationStation rs : stations) {
            if (rs.isBusy() && rs.getTimeLeft() == 0 && !rs.hasWrittenResult()) {
                finished.add(rs);
            }
        }
        return finished;
    }

    private void broadcastToStations(List<ReservationStation> stations, String tag, double value) {
        for (ReservationStation rs : stations) {
            if (rs.isBusy()) {
                rs.receiveCDB(tag, value); // Clears Qj or Qk if they match 'tag'
            }
        }
    }
    
    private int getLatencyForOp(String op) {
        if (op.startsWith("ADD") || op.startsWith("SUB")) return addLatency;
        if (op.startsWith("MUL")) return mulLatency;
        if (op.startsWith("DIV")) return divLatency;
        if (op.startsWith("L")) return loadLatency;
        return 1;
    }
    
    private List<ReservationStation> getTargetList(String op) {
        if (op.startsWith("ADD") || op.startsWith("SUB")) return addStations;
        if (op.startsWith("MUL") || op.startsWith("DIV")) return mulStations;
        if (op.startsWith("L")) return loadBuffers;
        if (op.startsWith("S")) return storeBuffers;
        return addStations; // default
    }
    
    // Getters for GUI (Member 1)
    public int getCurrentCycle() { return currentCycle; }
    public List<ReservationStation> getReservationStations() {
        List<ReservationStation> all = new ArrayList<>();
        all.addAll(addStations);
        all.addAll(mulStations);
        all.addAll(loadBuffers);
        all.addAll(storeBuffers);
        return all;
    }
    public RegisterFile getFloatRegFile() { return floatRegFile; }
    public MemoryUnit getMemory() { return memory; }
}