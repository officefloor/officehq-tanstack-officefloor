package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class WithdrawRate {
    public void withdraw(@PathVariable("sheetId") String sheetId, @Val RateWithdrawInput rate, RateStore store) {
        store.withdraw(sheetId.trim(), rate.grade(), rate.from());
    }
}
