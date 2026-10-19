package net.officefloor.hq.app.claim;

/** Where a claim is in its life: DRAFT -> OPEN -> SETTLED, or DRAFT/OPEN -> VOID. Only OPEN and
 *  SETTLED claims count toward the totals; SETTLED and VOID are final. */
public enum ClaimStatus {
    DRAFT, OPEN, SETTLED, VOID
}
