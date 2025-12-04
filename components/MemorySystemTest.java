package Components;

/**
 * MemorySystemTest - Demonstrates Cache Hit/Miss behavior
 * Shows a cache miss followed by a cache hit on the same block
 */
public class MemorySystemTest {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   MEMORY SYSTEM TEST");
        System.out.println("========================================\n");

        // Configuration
        int memorySize = 1024;      // 1 KB main memory
        int cacheSize = 128;        // 128 bytes cache
        int blockSize = 16;         // 16 bytes per block
        int hitLatency = 1;         // 1 cycle on hit
        int missPenalty = 10;       // 10 cycles on miss

        // Create the memory system
        MemorySystem memory = new MemorySystem(memorySize, cacheSize, blockSize, hitLatency, missPenalty);

        System.out.println("\n========================================");
        System.out.println("   TEST 1: Integer Operations (LW)");
        System.out.println("========================================");

        // Store some integers in main memory
        memory.storeInt(100, 42);
        memory.storeInt(104, 1234);
        memory.storeInt(108, 5678);
        memory.storeInt(112, 9999);

        // First load - should be a CACHE MISS
        System.out.println("\n--- First Load (Expected: CACHE MISS) ---");
        MemoryResponse response1 = memory.load(100, 4);
        int value1 = response1.toInt();
        System.out.println("Value loaded: " + value1);
        System.out.println("Cycles taken: " + response1.getCyclesTaken());
        System.out.println("Expected cycles: " + (hitLatency + missPenalty));

        // Second load from the same block - should be a CACHE HIT
        System.out.println("\n--- Second Load from Same Block (Expected: CACHE HIT) ---");
        MemoryResponse response2 = memory.load(104, 4);
        int value2 = response2.toInt();
        System.out.println("Value loaded: " + value2);
        System.out.println("Cycles taken: " + response2.getCyclesTaken());
        System.out.println("Expected cycles: " + hitLatency);

        // Third load from the same block - should also be a CACHE HIT
        System.out.println("\n--- Third Load from Same Block (Expected: CACHE HIT) ---");
        MemoryResponse response3 = memory.load(108, 4);
        int value3 = response3.toInt();
        System.out.println("Value loaded: " + value3);
        System.out.println("Cycles taken: " + response3.getCyclesTaken());

        System.out.println("\n========================================");
        System.out.println("   TEST 2: Double Operations (L.D)");
        System.out.println("========================================");

        // Store some doubles in main memory
        memory.storeDouble(200, 3.14159);
        memory.storeDouble(208, 2.71828);
        memory.storeDouble(216, 1.41421);

        // First load - should be a CACHE MISS
        System.out.println("\n--- First Load (Expected: CACHE MISS) ---");
        MemoryResponse response4 = memory.load(200, 8);
        double doubleValue1 = response4.toDouble();
        System.out.println("Value loaded: " + doubleValue1);
        System.out.println("Cycles taken: " + response4.getCyclesTaken());

        // Second load from the same block - should be a CACHE HIT
        System.out.println("\n--- Second Load from Same Block (Expected: CACHE HIT) ---");
        MemoryResponse response5 = memory.load(208, 8);
        double doubleValue2 = response5.toDouble();
        System.out.println("Value loaded: " + doubleValue2);
        System.out.println("Cycles taken: " + response5.getCyclesTaken());

        System.out.println("\n========================================");
        System.out.println("   TEST 3: Cache Conflict");
        System.out.println("========================================");

        // Access an address that maps to a different cache block
        // With 8 cache blocks (128 / 16), addresses 100 and 228 map to different blocks
        // Block for address 100: 100/16 = 6, index = 6 % 8 = 6
        // Block for address 228: 228/16 = 14, index = 14 % 8 = 6 (same index, different tag!)
        
        memory.storeInt(228, 77777);
        
        System.out.println("\n--- Load from Address 228 (Expected: CACHE MISS, conflicts with block at 100) ---");
        MemoryResponse response6 = memory.load(228, 4);
        int value6 = response6.toInt();
        System.out.println("Value loaded: " + value6);
        System.out.println("Cycles taken: " + response6.getCyclesTaken());

        // Now if we access 100 again, it should be a MISS because it was evicted
        System.out.println("\n--- Load from Address 100 Again (Expected: CACHE MISS, was evicted) ---");
        MemoryResponse response7 = memory.load(100, 4);
        int value7 = response7.toInt();
        System.out.println("Value loaded: " + value7);
        System.out.println("Cycles taken: " + response7.getCyclesTaken());

        // Print statistics
        memory.printStatistics();

        System.out.println("\n========================================");
        System.out.println("   TEST 4: Write-Through Behavior");
        System.out.println("========================================");

        // Reset statistics for clarity
        memory.resetStatistics();
        memory.invalidateCache();

        // Store and load to demonstrate write-through
        System.out.println("\n--- Store Integer at Address 300 ---");
        memory.storeInt(300, 88888);

        System.out.println("\n--- Load from Address 300 (Expected: CACHE MISS) ---");
        MemoryResponse response8 = memory.load(300, 4);
        int value8 = response8.toInt();
        System.out.println("Value loaded: " + value8);
        System.out.println("Cycles taken: " + response8.getCyclesTaken());

        System.out.println("\n--- Update Value at Address 300 ---");
        memory.storeInt(300, 99999);

        System.out.println("\n--- Load from Address 300 Again (Expected: CACHE HIT, updated value) ---");
        MemoryResponse response9 = memory.load(300, 4);
        int value9 = response9.toInt();
        System.out.println("Value loaded: " + value9);
        System.out.println("Cycles taken: " + response9.getCyclesTaken());

        // Print final statistics
        memory.printStatistics();

        System.out.println("\n========================================");
        System.out.println("   ALL TESTS COMPLETED!");
        System.out.println("========================================");
    }
}
