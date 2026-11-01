package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;

public class WithdrawRate {
    public void withdraw(@Val RateWithdrawal withdrawal, Rates rates) {
        rates.withdraw(withdrawal);
    }
}
