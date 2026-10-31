package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A validated split body; `into` is the new claim split off, set once the split is applied. */
public class ClaimSplit {
    private final BigDecimal amount;
    private final String user;
    private Claim into;

    public ClaimSplit(BigDecimal amount, String user) {
        this.amount = amount;
        this.user = user;
    }

    public BigDecimal amount() { return amount; }
    public String user() { return user; }
    public Claim into() { return into; }

    public void into(Claim into) { this.into = into; }
}
