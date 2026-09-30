package net.officefloor.hq.app;

/**
 * The request body for setting a client's currency: just the ISO currency code (e.g. USD or EUR).
 * Bound from the POST JSON body via {@code @RequestBody}.
 */
public class NewCurrency {

    private String currency;

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
