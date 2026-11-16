package net.officefloor.hq.app.claim;

/** A claim's status. Only OPEN, PENDING and SETTLED claims count toward claimed and the totals. PENDING is a claim a
 *  clerk settled above the clerk limit, waiting for a steward to approve (SETTLED) or reject (back to OPEN) it. */
public enum ClaimStatus {
    DRAFT, OPEN, PENDING, SETTLED, VOID
}
