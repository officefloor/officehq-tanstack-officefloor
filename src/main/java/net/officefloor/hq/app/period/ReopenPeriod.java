package net.officefloor.hq.app.period;

import java.time.LocalDate;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Reopens a closed period, dropping its fixed due-total so it is live again. Refused when the user is
 *  not a steward on the sheet, there is no such period, or it is not closed. */
public class ReopenPeriod {
    public void reopen(@PathVariable("sheetId") String sheetId, @PathVariable("periodName") String periodName,
            @Val CloseRequest request, PeriodStore store, RoleStore roles, Out<PeriodResponse> reopened)
            throws InvalidSheetException {
        String sheet = sheetId.trim();
        if (roles.find(sheet, request.user()) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward can reopen a period");
        }
        PeriodResponse period = store.find(sheet, periodName.trim());
        if (period == null) {
            throw new InvalidSheetException("No such period: " + periodName.trim());
        }
        if (!period.closed() || !store.reopen(sheet, period.name())) {
            throw new InvalidSheetException("The period " + period.name() + " is not closed");
        }
        reopened.set(PeriodResponse.of(period.name(), LocalDate.parse(period.from()), LocalDate.parse(period.to())));
    }
}
