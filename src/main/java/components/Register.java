package components;

public class Register {
    public String name;
    public double value;
    public String Qi;
    
    public Register(String name) {
        this.name = name;
        this.Qi = null;
        this.value = 0.0;
    }
    
    // JavaBean getters for JavaFX PropertyValueFactory
    public String getName() { return name; }
    public double getValue() { return value; }
    public String getQi() { return Qi; }
}