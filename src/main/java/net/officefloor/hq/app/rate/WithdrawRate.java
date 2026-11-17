package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Withdraws the newest version of the grade's rate for the from-date; refused when there is no such rate. */
public class WithdrawRate {
    public void withdraw(@PathVariable("sheetId") String sheetId, @Val RateWithdrawValues values, RateRepository rates,
            Out<RateWithdrawn> withdrawn) throws InvalidRateException {
        String sheet = sheetId.trim();
        if (!rates.has(sheet, values.grade(), values.from())) {
            throw new InvalidRateException("There is no such rate to withdraw");
        }
        withdrawn.set(new RateWithdrawn(sheet, values.grade(), values.from(),
                rates.withdraw(sheet, values.grade(), values.from())));
    }
}
