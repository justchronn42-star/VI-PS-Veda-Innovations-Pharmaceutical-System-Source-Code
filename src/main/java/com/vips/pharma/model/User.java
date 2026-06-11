package com.vips.pharma.model;

import javafx.beans.property.*;

/**
 * Represents a system user stored in the {@code users} table.
 * Passwords are stored as BCrypt hashes – never in plain text.
 */
public class User {

    private final IntegerProperty id           = new SimpleIntegerProperty();
    private final StringProperty  username     = new SimpleStringProperty();
    private final StringProperty  passwordHash = new SimpleStringProperty();
    private final ObjectProperty<Role> role    = new SimpleObjectProperty<>();
    private final BooleanProperty active       = new SimpleBooleanProperty(true);

    public User() {}

    public User(int id, String username, String passwordHash, Role role, boolean active) {
        this.id.set(id);
        this.username.set(username);
        this.passwordHash.set(passwordHash);
        this.role.set(role);
        this.active.set(active);
    }

    /* ── id ── */
    public int getId()              { return id.get(); }
    public void setId(int v)        { id.set(v); }
    public IntegerProperty idProperty() { return id; }

    /* ── username ── */
    public String getUsername()         { return username.get(); }
    public void setUsername(String v)   { username.set(v); }
    public StringProperty usernameProperty() { return username; }

    /* ── passwordHash ── */
    public String getPasswordHash()           { return passwordHash.get(); }
    public void setPasswordHash(String v)     { passwordHash.set(v); }
    public StringProperty passwordHashProperty() { return passwordHash; }

    /* ── role ── */
    public Role getRole()               { return role.get(); }
    public void setRole(Role v)         { role.set(v); }
    public ObjectProperty<Role> roleProperty() { return role; }

    /* ── active ── */
    public boolean isActive()           { return active.get(); }
    public void setActive(boolean v)    { active.set(v); }
    public BooleanProperty activeProperty() { return active; }

    @Override
    public String toString() { return username.get(); }
}
