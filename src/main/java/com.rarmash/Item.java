package com.rarmash;

class Item {
    private final String name;
    private final String exterior;
    private final double price;

    public Item(String name, String exterior, double price) {
        this.name = name;
        this.exterior = exterior;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public String getExterior() {
        return exterior;
    }

    public double getPrice() {
        return price;
    }
}
