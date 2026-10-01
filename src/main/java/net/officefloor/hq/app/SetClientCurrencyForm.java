package net.officefloor.hq.app;

/** The request body for setting a client's billing currency: which client, and the currency to set. */
public class SetClientCurrencyForm {

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
