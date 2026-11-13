package net.officefloor.hq.app.claim;

/**
 * Where a claim is in its life: it starts DRAFT, is opened and then settled; a clerk settling one whose due is above
 * the clerk limit makes it PENDING instead, until a steward approves it (SETTLED) or rejects it (OPEN again). A DRAFT,
 * OPEN or PENDING claim can be voided. Only a DRAFT can be edited. SETTLED and VOID are final. Only OPEN, PENDING and
 * SETTLED claims count toward the totals.
 */
public enum ClaimStatus {
    DRAFT, OPEN, PENDING, SETTLED, VOID;

    /** Whether a claim with this status is open: OPEN, or PENDING (handled like an open claim). */
    public boolean open() {
        return this == OPEN || this == PENDING;
    }
}
