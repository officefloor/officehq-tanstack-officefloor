package net.officefloor.hq.app;

/** The request body for sending an invoice: the id of the invoice to send. */
public class SendInvoiceForm {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
