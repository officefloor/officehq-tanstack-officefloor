package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A clerk may not settle a claim whose due is above the sheet's clerk limit. */
public class ClerkLimitException extends Exception {
    public ClerkLimitException(String user, BigDecimal due, BigDecimal limit) {
        super("A claim due " + due.toPlainString() + " is above the clerk limit " + limit.toPlainString()
                + ": " + user + " is a clerk and cannot settle it");
    }
}
