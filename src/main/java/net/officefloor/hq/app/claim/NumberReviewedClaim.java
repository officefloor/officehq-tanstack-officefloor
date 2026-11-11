package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Gives a reviewed claim its number (the sheet's next, or the next within its period); refused when
 * another request reviewed or voided it first.
 */
public class NumberReviewedClaim {
    public void number(@Val Sheet sheet, @Val Claim claim, ClaimRepository repository, SettingsRepository settings,
            PeriodRepository periods) throws InvalidClaimStatusException {
        if (!repository.markReviewed(claim)) throw new InvalidClaimStatusException(ClaimStatus.REVIEW, ClaimStatus.OPEN);
        claim.setNumber(ClaimNumbers.next(sheet, claim, settings.find(sheet.id()), periods, repository));
    }
}
