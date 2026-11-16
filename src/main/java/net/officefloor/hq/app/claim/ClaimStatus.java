package net.officefloor.hq.app.claim;

/** A claim's status. Only OPEN and SETTLED claims count toward claimed and the totals. */
public enum ClaimStatus {
    DRAFT, OPEN, SETTLED, VOID
}
