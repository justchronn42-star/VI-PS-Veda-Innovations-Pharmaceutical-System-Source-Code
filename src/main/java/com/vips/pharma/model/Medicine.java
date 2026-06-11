package com.vips.pharma.model;

import javafx.beans.property.*;
import java.time.LocalDate;

public class Medicine {
    private final IntegerProperty id    = new SimpleIntegerProperty();
    private final StringProperty  name  = new SimpleStringProperty();
    private final IntegerProperty stock = new SimpleIntegerProperty();
    private final DoubleProperty  price = new SimpleDoubleProperty();
    private final ObjectProperty<LocalDate> expiryDate = new SimpleObjectProperty<>();

    public Medicine() {}

    public Medicine(int id, String name, int stock, double price) {
        this.id.set(id);
        this.name.set(name);
        this.stock.set(stock);
        this.price.set(price);
    }

    public Medicine(int id, String name, int stock, double price, LocalDate expiryDate) {
        this(id, name, stock, price);
        this.expiryDate.set(expiryDate);
    }

    public int getId()              { return id.get(); }
    public void setId(int v)        { id.set(v); }
    public IntegerProperty idProperty() { return id; }

    public String getName()         { return name.get(); }
    public void setName(String v)   { name.set(v); }
    public StringProperty nameProperty() { return name; }

    public int getStock()           { return stock.get(); }
    public void setStock(int v)     { stock.set(v); }
    public IntegerProperty stockProperty() { return stock; }

    public double getPrice()        { return price.get(); }
    public void setPrice(double v)  { price.set(v); }
    public DoubleProperty priceProperty() { return price; }

    public LocalDate getExpiryDate()            { return expiryDate.get(); }
    public void setExpiryDate(LocalDate v)       { expiryDate.set(v); }
    public ObjectProperty<LocalDate> expiryDateProperty() { return expiryDate; }

    public boolean isExpired() {
        LocalDate d = expiryDate.get();
        return d != null && !d.isAfter(LocalDate.now());
    }

    @Override public String toString() { return name.get(); }
}
