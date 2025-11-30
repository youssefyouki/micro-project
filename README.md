# Tomasulo Simulator - Build & Run Guide

## ✅ What Was Fixed

1. **Syntax Error**: Fixed `[cite_start]` marker in `CommonDataBus.java`
2. **Duplicate Classes**: Separated `Register` into its own file
3. **Java Version Mismatch**: Downgraded JavaFX from 21 to 11.0.2 to match Java 11
4. **Maven Project Structure**: Reorganized files into proper Maven `src/main/java` directory structure
5. **Duplicate Imports**: Removed duplicate `Instruction` and `MemoryUnit` classes
6. **Missing Methods**: Added required methods to `ReservationStation` and `CacheBlock`
7. **Package Organization**: 
   - `view/` - UI components
   - `simulation/` - Engine and CDB
   - `components/` - Hardware components (RS, Cache, Registers, Instructions, etc.)

## 🚀 How to Run

### Option 1: Using Maven (Recommended)
```powershell
cd "C:\Users\hanee\OneDrive\Desktop\micro-project"
mvn clean compile
mvn javafx:run
```

### Option 2: Using the Batch File
Double-click `run.bat` in the project root

### Option 3: Manual Command
```powershell
cd "C:\Users\hanee\OneDrive\Desktop\micro-project"
java -cp "target\classes" view.MainUI
```

## 📁 Project Structure

```
src/main/java/
├── view/
│   └── MainUI.java           # JavaFX GUI
├── simulation/
│   ├── SimulatorEngine.java  # Main simulation engine
│   └── CommonDataBus.java    # CDB for result broadcasting
└── components/
    ├── ReservationStation.java  # RS units
    ├── RegisterFile.java        # Register file
    ├── Register.java           # Individual register
    ├── CacheBlock.java         # Cache block
    ├── Instruction.java        # Instruction format
    ├── InstructionQueue.java   # Instruction queue
    └── MemoryUnit.java         # Memory/Cache controller
```

## 🔧 Configuration

When you run the application, you'll see a configuration screen:
- **ADD/SUB Latency**: (default 2 cycles)
- **MUL Latency**: (default 10 cycles)
- **DIV Latency**: (default 40 cycles)
- **LOAD/STORE Latency**: (default 2 cycles)
- **Cache Size**: (default 64 bytes)
- **Block Size**: (default 4 bytes)

## ✨ Features

- Tomasulo algorithm simulator
- Visualization of Reservation Stations
- Register file display
- Cache block monitoring
- Cycle-by-cycle execution
- Common Data Bus (CDB) for result broadcasting

## 🛠️ Build Tool: Maven

The project uses Maven for dependency management and building. Key commands:

```powershell
mvn clean          # Clean build directory
mvn compile        # Compile sources
mvn package        # Create JAR
mvn javafx:run     # Run JavaFX application
```

## ✅ Compilation Status

**Current Status**: ✅ BUILD SUCCESS

All 10 source files compile successfully with warnings about unchecked operations (which are expected with raw TableView types).

## 📝 Notes

- Java 11+ is required
- JavaFX 11.0.2 is automatically managed by Maven
- The application is ready to simulate Tomasulo algorithm execution
