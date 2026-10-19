package net.officefloor.hq.app.claim;

/** The number an audit line about a claim carries; null when the claim has none. */
public record ClaimNumber(Integer value) {

    /** The audit field for the number, to go at the end of the line; empty when there is none. */
    public String auditField() {
        return value == null ? "" : " number=" + value;
    }
}
