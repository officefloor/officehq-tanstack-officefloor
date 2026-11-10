package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Keeps the sheet's figures before the change, so {@link AuditRollup} can tell which moved. */
public class TakeRollup {
    public void take(@Val Sheet sheet, PersonRepository people, ClaimRepository claims, RateRepository rates,
            SettingsRepository settings,
            Out<Rollup> rollupOut) {
        rollupOut.set(Rollup.of(sheet.id(), sheet.today(), people, claims, rates,
                settings.find(sheet.id())));
    }
}
