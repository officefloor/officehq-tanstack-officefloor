package net.officefloor.hq.app;

/** The request body for marking an invoice paid: the id of the invoice to pay. */
public class PayInvoiceForm {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
