package net.officefloor.hq.app;

/**
 * Request body for {@code POST /api/lineitems/remove}: the id of the line item to take off its
 * invoice. Bound from the JSON payload by the Spring MVC {@code @RequestBody} argument resolver.
 */
public class RemoveLineItem {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
