package net.officefloor.hq.app;

/**
 * The request body for removing a line item from an invoice: which line to drop. The owning invoice
 * is derived from the line itself, so the caller only needs to name the line.
 */
public class RemoveLineItemForm {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
