package net.officefloor.hq.app.paste;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimAdding;
import net.officefloor.hq.app.claim.ClaimLogStore;
import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.hq.app.holiday.HolidayStore;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds each pasted claim in order as the claim form would, a DRAFT recorded as CLAIM_ADDED. */
public class SavePasted {
    public void save(@Val PasteInput pasted, ClaimStore store,
            SettingStore settings, ClaimLogStore log, PeriodStore periods, RollupService rollups, Audit audit, HolidayStore holidays, Out<PasteResult> result) {
        for (PastedClaim p : pasted.claims()) {
            ClaimAdding.add(p.claim(), p.person(), store, settings, log, periods, rollups, audit, holidays);
        }
        result.set(new PasteResult(pasted.claims().size()));
    }
}
