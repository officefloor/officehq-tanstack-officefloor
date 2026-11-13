package net.officefloor.hq.app.claim;

/**
 * Where a claim is in its life: it starts DRAFT, is opened and then settled; a DRAFT or OPEN claim
 * can be voided. SETTLED and VOID are final. Only OPEN and SETTLED claims count toward the totals.
 */
public enum ClaimStatus {
    DRAFT, OPEN, SETTLED, VOID
}
