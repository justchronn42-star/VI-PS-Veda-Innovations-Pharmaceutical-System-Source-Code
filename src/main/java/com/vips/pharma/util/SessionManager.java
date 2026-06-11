package com.vips.pharma.util;

import com.vips.pharma.model.User;

/**
 * Application-wide singleton that holds the currently authenticated user.
 * Set after a successful login; cleared on logout.
 */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() { return INSTANCE; }

    /** Store the authenticated user at login time. */
    public void login(User user)  { this.currentUser = user; }

    /** Clear the session (called on logout). */
    public void logout()          { this.currentUser = null; }

    /** The currently logged-in user, or {@code null} if not authenticated. */
    public User getCurrentUser()  { return currentUser; }

    /** Convenience: username for audit log stamping. */
    public String getUsername() {
        return currentUser != null ? currentUser.getUsername() : "SYSTEM";
    }

    /** Convenience: true if a user is authenticated. */
    public boolean isLoggedIn()   { return currentUser != null; }
}
