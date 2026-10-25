package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;

/** Refuses a reference another claim on the sheet already carries, comparing without regard to case. */
final class RefuseUsedRef {

    private RefuseUsedRef() {
    }

    /** Refuse {@code ref} (null: none, never refused) when a claim on the sheet other than {@code exceptId} carries it. */
    static void refuse(String sheetId, String ref, long exceptId, ClaimRepository repository) throws BadRequestException {
        if (ref != null && repository.refUsed(sheetId, ref, exceptId)) {
            throw used(ref);
        }
    }

    static BadRequestException used(String ref) {
        return new BadRequestException("Reference " + ref + " is already used on this sheet");
    }
}
