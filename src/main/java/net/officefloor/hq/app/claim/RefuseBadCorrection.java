package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;

import net.officefloor.hq.app.BadRequestException;

/**
 * A claim with a negative amount is a correction: it must name, by number, a SETTLED claim of the same person;
 * any other claim names none. A correction may not exceed what remains of the claim it corrects.
 */
final class RefuseBadCorrection {

    private RefuseBadCorrection() {
    }

    static void refuse(ClaimInput input, String sheetId, long personId, ClaimRepository repository)
            throws BadRequestException {
        refuse(input, sheetId, personId, -1, BigDecimal.ZERO, repository);
    }

    /**
     * As {@link #refuse(ClaimInput, String, long, ClaimRepository)}, the claim {@code exceptId} (the one being edited; -1
     * for none) left out of the corrections already made, and {@code pending} (the not yet stored corrections against the
     * same claim, as a negative sum) counted in.
     */
    static void refuse(ClaimInput input, String sheetId, long personId, long exceptId, BigDecimal pending,
            ClaimRepository repository) throws BadRequestException {
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
        BigDecimal remaining = repository.correctableRemaining(sheetId, personId, input.corrects(), exceptId).add(pending);
        if (input.amount().negate().compareTo(remaining) > 0) {
            throw new BadRequestException("A correction may not exceed what remains of the claim it corrects ("
                    + remaining.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP).toPlainString() + ")");
        }
    }
}
