package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;

public class SaveRate {
    public void save(@Val Rate rate, Rates rates) {
        rates.save(rate);
    }
}
