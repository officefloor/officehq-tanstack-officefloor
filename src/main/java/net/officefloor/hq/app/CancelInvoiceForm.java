package net.officefloor.hq.app;

/** The request body for cancelling an invoice: the id of the invoice to void. */
public class CancelInvoiceForm {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
