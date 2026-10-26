package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;

public class ApplyRate {
    public void apply(@Val Rate rate, Rates rates) {
        rates.save(rate);
    }
}
