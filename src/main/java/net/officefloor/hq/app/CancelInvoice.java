package net.officefloor.hq.app;

/**
 * Request body for {@code POST /api/invoices/cancel}: the id of the invoice to cancel (void). Bound
 * from the JSON payload by the Spring MVC {@code @RequestBody} argument resolver.
 */
public class CancelInvoice {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
