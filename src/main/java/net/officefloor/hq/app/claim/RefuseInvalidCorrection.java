package net.officefloor.hq.app.claim;

/** The check, shared by an add and an edit, that a correction names a SETTLED claim of the same person. */
final class RefuseInvalidCorrection {

    private RefuseInvalidCorrection() {
    }

    static void refuse(ClaimInput input, String sheetId, long personId, Claims claims) throws InvalidClaimException {
        if (input.corrects() == null) {
            return;
        }
        Claim corrected = claims.findByNumber(sheetId, personId, input.corrects());
        if (corrected == null || corrected.status() != ClaimStatus.SETTLED) {
            throw new InvalidClaimException(
                    "A correction must name a settled claim of the same person: " + input.corrects() + " is not one.");
        }
    }
}
