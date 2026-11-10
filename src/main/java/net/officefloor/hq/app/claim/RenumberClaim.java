package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Gives a claim moved to another person the next number in the sequence it would be numbered in
 * (the sheet's, or its period's); its old number is never used again. A draft has none and gets none.
 */
public class RenumberClaim {
    public void renumber(@Val Sheet sheet, @Val Claim claim, ClaimRepository repository, SettingsRepository settings,
            PeriodRepository periods) {
        give(sheet, claim, repository, settings, periods);
    }

    static void give(Sheet sheet, Claim claim, ClaimRepository repository, SettingsRepository settings,
            PeriodRepository periods) {
        if (claim.getNumber() == null) return;
        claim.setNumber(ClaimNumbers.next(sheet, claim, settings.find(sheet.id()), periods, repository));
    }
}
