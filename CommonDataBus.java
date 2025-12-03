import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;

public class CommonDataBus {
    private static CommonDataBus instance;
    private String publisherTag; // Who is writing?
    private double publisherValue; // What are they writing?
    private boolean busy = false; // Only one can publish per cycle

    // Listeners: RegisterFile, ReservationStations, Buffers
    public interface Listener {
        void onCDBBroadcast(String tag, double value);
    }
    private final List<Listener> listeners = new ArrayList<>();
    // Queue of pending publish requests. Resolve one per cycle.
    private final Queue<PublishRequest> pending = new LinkedList<>();

    private static class PublishRequest {
        public final String source; // station name
        public final double value;
        public final long time; // insertion time for tie-breaking

        PublishRequest(String source, double value, long time) {
            this.source = source;
            this.value = value;
            this.time = time;
        }
    }

    private CommonDataBus() {}

    public static CommonDataBus getInstance() {
        if (instance == null) {
            instance = new CommonDataBus();
        }
        return instance;
    }

    public void registerListener(Listener l) {
        listeners.add(l);
    }

    // Only one station can publish per cycle
    // Immediate publish (keeps compatibility). Returns true if published now.
    public boolean publish(String tag, double value) {
        if (busy) return false; // Already published this cycle
        this.publisherTag = tag;
        this.publisherValue = value;
        busy = true;
        // Broadcast to all listeners
        for (Listener l : listeners) {
            l.onCDBBroadcast(tag, value);
        }
        return true;
    }

    // Request a publish to be arbitrated. Always enqueues; caller should set pending flag.
    public void requestPublish(String source, double value) {
        pending.add(new PublishRequest(source, value, System.nanoTime()));
    }

    // Resolve the next pending publish (one per cycle). Returns true if a publish happened.
    public boolean resolveNextPublish() {
        if (busy) return false; // Already published this cycle
        PublishRequest req = pending.poll();
        if (req == null) return false;
        this.publisherTag = req.source;
        this.publisherValue = req.value;
        busy = true;
        for (Listener l : listeners) {
            l.onCDBBroadcast(req.source, req.value);
        }
        return true;
    }

    // Call at the end of each cycle to allow new publish
    public void resetBus() {
        busy = false;
        publisherTag = null;
    }

    // Alias for resetBus to maintain compatibility
    public void clear() {
        resetBus();
    }
}