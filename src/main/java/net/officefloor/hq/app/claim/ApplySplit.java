package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/**
 * Splits an amount (more than 0.00, less than the claim's) off an OPEN claim that is not deferred and not dated inside a
 * closed period; only the roles the sheet's open permission allows split a claim, as the new claim is theirs to open.
 * The claim keeps the rest of its amount and loses any figure a statement bound it to.
 */
public class ApplySplit {
    public void apply(@Val ValidSplit request, @Val Claim claim, Sheets sheets, Periods periods, Settings settings)
            throws ClaimStatusRefusedException {
        if (!settings.allows(claim.getSheetId(), request.user(), "open")) {
            throw new ClaimStatusRefusedException("Your role may not open a claim, so may not split one");
        }
        if (claim.getStatus() != ClaimStatus.OPEN) {
            throw new ClaimStatusRefusedException("Only an OPEN claim can be split");
        }
        if (claim.isDeferredOn(sheets.today(claim.getSheetId()))) {
            throw new ClaimStatusRefusedException("A deferred claim cannot be split");
        }
        if (periods.isClosed(claim.getSheetId(), claim.getDate())) {
            throw new ClaimStatusRefusedException("No claim dated inside a closed period can be split");
        }
        if (request.amount().compareTo(claim.getAmount()) >= 0) {
            throw new ClaimStatusRefusedException("The amount to split off must be less than the claim's amount");
        }
        claim.setAmount(claim.getAmount().subtract(request.amount()));
        claim.setStated(false);
        claim.setStatedDue(null);
    }
}
