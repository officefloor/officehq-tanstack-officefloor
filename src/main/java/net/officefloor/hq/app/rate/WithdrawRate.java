package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Removes the newest version of the sheet's own rate for the grade and from-date; refused (400) when it has none. */
public class WithdrawRate {
    public void withdraw(@Val RateWithdrawRequest request, @Val Sheet sheet, RateRepository repository,
            Out<RateWithdrawn> withdrawn) throws InvalidRateException {
        LocalDate from = request.from() == null ? null : LocalDate.parse(request.from());
        int left = repository.withdraw(sheet.id(), request.grade(), from);
        if (left < 0) {
            throw new InvalidRateException("The grade " + request.grade() + " has no rate from "
                    + (from == null ? "the start" : from) + " to withdraw.");
        }
        withdrawn.set(new RateWithdrawn(sheet.id(), request.grade(), from, left));
    }
}
