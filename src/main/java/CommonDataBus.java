public class CommonDataBus {
    private static CommonDataBus instance;
    private String publisherTag; // Who is writing?
    private double publisherValue; // What are they writing?

    // TODO (Member 6): Logic to allow only ONE station to publish per cycle [cite: 43]
    public boolean publish(String tag, double value) {
        this.publisherTag = tag;
        this.publisherValue = value;
        return true;
    }
}