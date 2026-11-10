package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheet;

/**
 * The number a claim being opened takes: the sheet's next (1, 2, ...), or, while the sheet's period
 * numbering from is set and its today is on or after it, the next within the claim's period (the one containing its due-by)
 * (<period name>-<n>), or X-<n> when the claim is in no period.
 */
public final class ClaimNumbers {
    private ClaimNumbers() {}

    public static String next(Sheet sheet, Claim claim, Settings settings, PeriodRepository periods, ClaimRepository claims) {
        if (!settings.numbersByPeriod(sheet.today())) return String.valueOf(claims.nextNumber(sheet.id()));
        Period period = periods.findAll(sheet.id()).stream()
                .filter((p) -> p.contains(claim.getDueBy())).findFirst().orElse(null);
        if (period == null) return "X-" + claims.nextNumberIn(sheet.id(), "");
        return period.name() + "-" + claims.nextNumberIn(sheet.id(), period.name());
    }
}
