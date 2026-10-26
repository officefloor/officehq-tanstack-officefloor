package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class RefuseClosedNewInput {
    // The date a claim is being added to (a blank date is the entry date, else the sheet's today) must not be in a closed period.
    public void refuse(@PathVariable("sheetId") String sheetId, @Val ClaimInput input, Settings settings, Periods periods)
            throws ClaimInClosedPeriodException {
        String sheet = sheetId.trim();
        LocalDate date = input.date() != null ? input.date() : settings.entryDate(sheet);
        Period closed = periods.closedOn(sheet, date);
        if (closed != null) {
            throw new ClaimInClosedPeriodException("The period " + closed.name() + " is closed: no claim dated "
                    + date + " can be added or edited.");
        }
    }
}
