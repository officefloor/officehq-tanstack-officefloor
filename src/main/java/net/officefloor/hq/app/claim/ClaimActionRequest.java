package net.officefloor.hq.app.claim;

/**
 * What a status change submits: the reason, for a reject (ignored otherwise; null: none), and the token of the bulk
 * settle a settle is part of (ignored otherwise; null: taken one at a time), and the date a settle is taken as of
 * (ignored otherwise; empty: now, the sheet's today).
 */
public record ClaimActionRequest(String reason, String batch, String on) {

    public ClaimActionRequest(String reason, String batch) {
        this(reason, batch, "");
    }
}
