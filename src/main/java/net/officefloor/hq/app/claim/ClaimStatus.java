package net.officefloor.hq.app.claim;

/** A claim's status. Only OPEN, PENDING and SETTLED claims count toward claimed and the totals. PENDING is a claim a
 *  clerk settled above the clerk limit, waiting for a steward to approve (SETTLED) or reject (back to OPEN) it. DELETED is a
 *  deleted DRAFT: it counts nowhere and takes no action but a restore (DRAFT again) on the sheet-today it was deleted. */
public enum ClaimStatus {
    DRAFT, OPEN, PENDING, SETTLED, VOID, DELETED
}
