package net.officefloor.hq.app;

/**
 * Request body for {@code POST /api/invoices/send}: the id of the invoice to send. Bound from the
 * JSON payload by the Spring MVC {@code @RequestBody} argument resolver.
 */
public class SendInvoice {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
