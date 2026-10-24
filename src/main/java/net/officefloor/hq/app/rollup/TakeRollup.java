package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Notes the sheet's figures before the change, so the audit can tell afterwards which of them moved. */
public class TakeRollup {
    public void take(@PathVariable("sheetId") String sheetId, SheetRepository sheets, PersonRepository people,
            ClaimRepository claims, RateRepository rates, Out<Rollup> before) {
        before.set(Rollup.of(sheetId, sheets, people, claims, rates));
    }
}
