package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.Objects;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Moves the claim to its new date, keeping its due-by and any statement figure. Under period numbering, a claim whose
 * period changes takes the next number of its new period's sequence (its old number is never used again).
 */
public class RedateClaim {
    public void redate(@Val Claim claim, @Val Redating redating, SheetRepository sheets, SettingRepository settings,
            PeriodRepository periods, ClaimRepository repository, Out<Claim> redated) {
        Claim moved = claim.redateTo(redating.to(), claim.number());
        LocalDate from = settings.periodNumberingFrom(claim.sheetId());
        boolean numbering = from != null && !sheets.today(claim.sheetId()).isBefore(from);
        if (numbering && claim.number() != null
                && !Objects.equals(periodOf(claim, redating.from(), periods), periodOf(claim, redating.to(), periods))) {
            moved = claim.redateTo(redating.to(), ApplyClaimAction.nextNumber(moved, sheets, settings, periods));
        }
        repository.redate(moved);
        redated.set(moved);
    }

    /** The name of the period containing the date (null: none). */
    private static String periodOf(Claim claim, LocalDate date, PeriodRepository periods) {
        return periods.findAll(claim.sheetId()).stream().filter((p) -> p.contains(date)).map(Period::name)
                .findFirst().orElse(null);
    }
}
