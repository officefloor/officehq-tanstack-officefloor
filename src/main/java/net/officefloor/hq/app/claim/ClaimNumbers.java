package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheet;

/**
 * The number a claim being opened takes: the next within the fiscal year of its due-by, in the
 * sequence the sheet's tree shares (<FY>-<n>), or, while the sheet's period
 * numbering from is set and its today is on or after it, the next within the claim's period (the one containing its due-by)
 * (<period name>-<n>), or X-<n> when the claim is in no period.
 */
public final class ClaimNumbers {
    private ClaimNumbers() {}

    public static String next(Sheet sheet, Claim claim, Settings settings, PeriodRepository periods, ClaimRepository claims) {
        if (!settings.numbersByPeriod(sheet.today())) {
            String fiscalYear = settings.fiscalYearOf(claim.getDueBy());
            return fiscalYear + "-" + claims.nextNumberInFiscalYear(sheet.id(), fiscalYear);
        }
        Period period = periods.findAll(sheet.id()).stream()
                .filter((p) -> p.contains(claim.getDueBy())).findFirst().orElse(null);
        if (period == null) return "X-" + claims.nextNumberIn(sheet.id(), "");
        return period.name() + "-" + claims.nextNumberIn(sheet.id(), period.name());
    }
}
