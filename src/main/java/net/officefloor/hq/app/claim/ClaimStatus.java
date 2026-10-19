package net.officefloor.hq.app.claim;

/** Where a claim is in its life: DRAFT -> OPEN -> SETTLED, or DRAFT/OPEN/PENDING -> VOID. A clerk settling
 *  above the clerk limit moves an OPEN claim to PENDING instead, which a steward approves (SETTLED) or rejects
 *  (back to OPEN). Only OPEN, PENDING and SETTLED claims count toward the totals; SETTLED and VOID are final. */
public enum ClaimStatus {
    DRAFT, OPEN, PENDING, SETTLED, VOID;

    /** Whether a claim in this status is open: OPEN, or PENDING (which is handled like OPEN in every total). */
    public boolean isOpen() { return this == OPEN || this == PENDING; }
}
