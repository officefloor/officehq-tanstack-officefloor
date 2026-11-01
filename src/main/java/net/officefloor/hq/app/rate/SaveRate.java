package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

public class SaveRate {
    public void save(@Val Rate rate, Rates rates, Sheets sheets) {
        rates.save(rate, sheets.today(rate.sheetId()));
    }
}
