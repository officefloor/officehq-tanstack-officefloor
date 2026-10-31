package net.officefloor.hq.app.claim;

/** Where a claim is in its life: DRAFT → OPEN → SETTLED, and DRAFT or OPEN → VOID. */
public enum ClaimStatus {
    DRAFT, OPEN, SETTLED, VOID;

    /** Only OPEN and SETTLED claims count toward claimed, due-total and every total. */
    public boolean counts() {
        return this == OPEN || this == SETTLED;
    }
}
