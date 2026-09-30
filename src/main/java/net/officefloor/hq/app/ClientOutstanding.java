package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * How much one client still owes, keyed by the client's id — the shape the clients list reads to
 * offer "sort by how much each client owes". The {@code outstanding} figure is the sum of the dues
 * across every invoice raised for the client (across all their projects), the same per-invoice due
 * the client statement uses ({@link ProjectInvoice#getDue}), so the two always agree. Derived per
 * request in {@link ClientsOutstandingGet}; nothing new is stored.
 */
public class ClientOutstanding {

    private final Long clientId;
    private final BigDecimal outstanding;

    public ClientOutstanding(Long clientId, BigDecimal outstanding) {
        this.clientId = clientId;
        this.outstanding = outstanding;
    }

    public Long getClientId() {
        return clientId;
    }

    public BigDecimal getOutstanding() {
        return outstanding;
    }
}
