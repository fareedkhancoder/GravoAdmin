package com.gravo.gravoadmin;

public class ManualSpecification {
    private String name;
    private String value;

    public ManualSpecification() {
        this.name = "";
        this.value = "";
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}