package net.officefloor.hq.app;

/**
 * Request body for {@code POST /api/invoices/pay}: the id of the invoice to mark paid. Bound from the
 * JSON payload by the Spring MVC {@code @RequestBody} argument resolver.
 */
public class PayInvoice {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
