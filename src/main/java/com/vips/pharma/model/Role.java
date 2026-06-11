package com.vips.pharma.model;

/**
 * User roles for VI-PS Pharma.
 *
 *  ADMIN       – full access (user management + all modules)
 *  INVENTORY_CLERK  – inventory CRUD + audit logs (no user management)
 *  CASHIER     – POS + receipts view only (read inventory, no edit/delete)
 */
public enum Role {
    ADMIN,
    INVENTORY_CLERK,
    CASHIER;

    /* ── convenience permission helpers ── */

    /** Can open the User Management screen */
    public boolean canManageUsers()    { return this == ADMIN; }

    /** Can add / edit / delete medicines */
    public boolean canEditInventory()  { return this == ADMIN || this == INVENTORY_CLERK; }

    /** Can view inventory list (all roles) */
    public boolean canViewInventory()  { return true; }

    /** Can process a sale / use the POS */
    public boolean canProcessSale()    { return this == ADMIN || this == CASHIER; }

    /** Can view receipts */
    public boolean canViewReceipts()   { return true; }

    /** Can view audit logs */
    public boolean canViewAudit()      { return this == ADMIN || this == INVENTORY_CLERK; }

    /** Human-readable display name */
    public String displayName() {
        return switch (this) {
            case ADMIN       -> "Administrator";
            case INVENTORY_CLERK  -> "Inventory Clerk";
            case CASHIER     -> "Cashier";
        };
    }
}
