public class CommonDataBus {
    private static CommonDataBus instance;
    private String publisherTag;
    private double publisherValue;
    private boolean occupiedThisCycle = false;

    private CommonDataBus() {}

    public static synchronized CommonDataBus getInstance() {
        if (instance == null) instance = new CommonDataBus();
        return instance;
    }

    public synchronized boolean publish(String tag, double value) {
        if (occupiedThisCycle) return false;
        this.publisherTag = tag;
        this.publisherValue = value;
        this.occupiedThisCycle = true;
        return true;
    }

    public synchronized String getPublisherTag() { return publisherTag; }
    public synchronized double getPublisherValue() { return publisherValue; }

    public synchronized void clear() {
        this.publisherTag = null;
        this.publisherValue = 0.0;
        this.occupiedThisCycle = false;
    }
}