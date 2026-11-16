package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;

public class WithdrawRate {
    public void withdraw(@Val RateWithdrawal withdrawal, RateRepository rates) {
        rates.withdraw(withdrawal.sheetId(), withdrawal.grade(), withdrawal.from());
    }
}
