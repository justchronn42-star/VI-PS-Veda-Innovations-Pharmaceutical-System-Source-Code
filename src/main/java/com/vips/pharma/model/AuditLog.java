package com.vips.pharma.model;

import javafx.beans.property.*;

public class AuditLog {
    private final IntegerProperty id       = new SimpleIntegerProperty();
    private final StringProperty  action   = new SimpleStringProperty();
    private final StringProperty  username = new SimpleStringProperty();
    private final StringProperty  dateTime = new SimpleStringProperty();

    public AuditLog() {}

    public AuditLog(int id, String action, String username, String dateTime) {
        this.id.set(id);
        this.action.set(action);
        this.username.set(username != null ? username : "SYSTEM");
        this.dateTime.set(dateTime);
    }

    // backward-compat constructor (no username)
    public AuditLog(int id, String action, String dateTime) {
        this(id, action, "SYSTEM", dateTime);
    }

    public int getId()                       { return id.get(); }
    public IntegerProperty idProperty()      { return id; }

    public String getAction()                { return action.get(); }
    public StringProperty actionProperty()   { return action; }

    public String getUsername()              { return username.get(); }
    public StringProperty usernameProperty() { return username; }

    public String getDateTime()              { return dateTime.get(); }
    public StringProperty dateTimeProperty() { return dateTime; }
}
