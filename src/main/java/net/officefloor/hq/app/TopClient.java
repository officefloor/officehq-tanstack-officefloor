package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * One entry in the home dashboard's "top clients" panel: a client and how much they still owe,
 * carrying the client's {@code name} so the panel renders without a second lookup. The
 * {@code outstanding} figure is the sum of the dues across every invoice raised for the client, the
 * same per-invoice due the client statement and the outstanding list use
 * ({@link ProjectInvoice#getDue}), so the three always agree. Derived per request in
 * {@link DashboardTopClientsGet}; nothing new is stored.
 */
public class TopClient {

    private final Long clientId;
    private final String name;
    private final BigDecimal outstanding;
    private final String currency;

    public TopClient(Long clientId, String name, BigDecimal outstanding, String currency) {
        this.clientId = clientId;
        this.name = name;
        this.outstanding = outstanding;
        this.currency = currency;
    }

    public Long getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getOutstanding() {
        return outstanding;
    }

    /** The currency this client pays in (an ISO code, e.g. USD or EUR); their figure renders in it. */
    public String getCurrency() {
        return currency;
    }
}
