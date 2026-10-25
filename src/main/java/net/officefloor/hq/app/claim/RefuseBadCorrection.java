package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;

/**
 * A claim with a negative amount is a correction: it must name, by number, a SETTLED claim of the same person;
 * any other claim names none.
 */
final class RefuseBadCorrection {

    private RefuseBadCorrection() {
    }

    static void refuse(ClaimInput input, String sheetId, long personId, ClaimRepository repository)
            throws BadRequestException {
        if (input.amount().signum() >= 0) {
            if (input.corrects() != null) {
                throw new BadRequestException("Only a correction (a negative amount) names a claim it corrects");
            }
            return;
        }
        if (input.corrects() == null) {
            throw new BadRequestException("A negative amount is a correction: name the settled claim it corrects");
        }
        if (!repository.settledNumbered(sheetId, personId, input.corrects())) {
            throw new BadRequestException("A correction names a SETTLED claim of the same person");
        }
    }
}
