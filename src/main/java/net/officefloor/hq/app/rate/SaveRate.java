package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;

public class SaveRate {
    public void save(@Val Rate rate, RateRepository repository) {
        repository.save(rate);
    }
}
