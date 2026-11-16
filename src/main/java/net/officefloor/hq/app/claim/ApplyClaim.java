package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Applies the edited values; a claim dated inside a closed period, or moved into one, is refused. */
public class ApplyClaim {
    public void apply(@Val Claim claim, @Val ClaimValues values, SheetRepository sheets, PeriodRepository periods,
            Out<Claim> changed) throws InvalidClaimException {
        LocalDate date = values.date() != null ? values.date() : sheets.today(claim.sheetId());
        if (periods.closedOn(claim.sheetId(), claim.date()) || periods.closedOn(claim.sheetId(), date)) {
            throw new InvalidClaimException("The claim's date is inside a closed period");
        }
        changed.set(new Claim(claim.id(), claim.sheetId(), claim.personId(), values.amount(), date, values.note(), claim.status(),
                claim.number(), claim.due(), claim.openedBy(), claim.stated(), claim.statedDue()));
    }
}
