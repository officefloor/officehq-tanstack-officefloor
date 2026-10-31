package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A validated re-settle: who takes it, and (once applied) the figure the claim was settled at before. */
public class ClaimResettle {
    private final String user;
    private BigDecimal old;

    public ClaimResettle(String user) {
        this.user = user;
    }

    public String user() { return user; }
    public BigDecimal old() { return old; }
    public void setOld(BigDecimal old) { this.old = old; }
}
