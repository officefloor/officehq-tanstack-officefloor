package net.officefloor.hq.app.claim;

/** The rule shared by the claim form's add and edit and the paste box: a reference is used once per sheet. */
final class RefuseUsedRef {

    private RefuseUsedRef() {
    }

    /** Refuses {@code ref} (null: none, always allowed) when a claim of the sheet other than {@code exceptId} carries it. */
    static void refuse(String ref, String sheetId, long exceptId, Claims claims) throws InvalidClaimException {
        if (ref != null && claims.refUsed(sheetId, ref, exceptId)) {
            throw new InvalidClaimException("The reference " + ref + " is already used on this sheet.");
        }
    }
}
