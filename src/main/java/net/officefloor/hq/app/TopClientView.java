package net.officefloor.hq.app;

import java.math.BigDecimal;

/**
 * One row of {@code GET /api/dashboard/top-clients}: a client the home screen ranks by how much it
 * owes — the client's id, its name, and the {@code amount} it still owes (its outstanding total, the
 * same per-client derivation {@link ClientsGet} uses for the clients-page "outstanding" sort). A
 * read-only aggregate joined in {@link DashboardTopClientsGet}; no entity of its own.
 */
public class TopClientView {

    private final long clientId;
    private final String name;
    private final BigDecimal amount;

    public TopClientView(long clientId, String name, BigDecimal amount) {
        this.clientId = clientId;
        this.name = name;
        this.amount = amount;
    }

    public long getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }

    /** How much this client still owes — its outstanding total across all its invoices. */
    public BigDecimal getAmount() {
        return amount;
    }
}
