package net.officefloor.hq.app;

/**
 * Request body for {@code POST /api/clients/currency}: the id of the client whose billing currency
 * is being set plus the new currency code. Bound from the JSON payload by the Spring MVC
 * {@code @RequestBody} argument resolver.
 */
public class SetClientCurrency {

    private Long id;

    private String currency;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
