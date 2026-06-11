package com.vips.pharma.model;

import javafx.beans.property.*;

public class Receipt {
    private final IntegerProperty id          = new SimpleIntegerProperty();
    private final StringProperty  medName     = new SimpleStringProperty();
    private final IntegerProperty quantity    = new SimpleIntegerProperty();
    private final DoubleProperty  totalAmount = new SimpleDoubleProperty();
    private final StringProperty  dateTime    = new SimpleStringProperty();
    private final StringProperty  processedBy = new SimpleStringProperty("SYSTEM");

    public Receipt() {}

    public Receipt(int id, String medName, int quantity, double totalAmount, String dateTime) {
        this.id.set(id);
        this.medName.set(medName);
        this.quantity.set(quantity);
        this.totalAmount.set(totalAmount);
        this.dateTime.set(dateTime);
    }

    public int getId()              { return id.get(); }
    public void setId(int v)        { id.set(v); }
    public IntegerProperty idProperty() { return id; }

    public String getMedName()      { return medName.get(); }
    public void setMedName(String v){ medName.set(v); }
    public StringProperty medNameProperty() { return medName; }

    public int getQuantity()        { return quantity.get(); }
    public void setQuantity(int v)  { quantity.set(v); }
    public IntegerProperty quantityProperty() { return quantity; }

    public double getTotalAmount()       { return totalAmount.get(); }
    public void setTotalAmount(double v) { totalAmount.set(v); }
    public DoubleProperty totalAmountProperty() { return totalAmount; }

    public String getDateTime()      { return dateTime.get(); }
    public void setDateTime(String v){ dateTime.set(v); }
    public StringProperty dateTimeProperty() { return dateTime; }

    public String getProcessedBy()       { return processedBy.get(); }
    public void setProcessedBy(String v) { processedBy.set(v); }
    public StringProperty processedByProperty() { return processedBy; }
}
