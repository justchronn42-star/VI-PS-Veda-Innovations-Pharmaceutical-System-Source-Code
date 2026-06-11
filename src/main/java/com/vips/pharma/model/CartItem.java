package com.vips.pharma.model;

public class CartItem {
    private final int medId;
    private final String medName;
    private int quantity;
    private final double unitPrice;
    private double total;

    public CartItem(int medId, String medName, int quantity, double unitPrice) {
        this.medId = medId;
        this.medName = medName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.total = quantity * unitPrice;
    }

    // Convenience constructor from Medicine model
    public CartItem(Medicine medicine, int quantity) {
        this(medicine.getId(), medicine.getName(), quantity, medicine.getPrice());
    }

    public int getMedId()       { return medId; }
    public String getMedName()  { return medName; }
    public int getQuantity()    { return quantity; }
    public double getUnitPrice(){ return unitPrice; }
    public double getTotal()    { return total; }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.total = quantity * this.unitPrice;
    }
}
