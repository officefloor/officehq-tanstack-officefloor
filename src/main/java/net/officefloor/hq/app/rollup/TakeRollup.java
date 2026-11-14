package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** First write step: the sheet's figures before the change, for {@link RecordRollup}. */
public class TakeRollup {
    public void take(@PathVariable("sheetId") String sheetId, PersonRepository people,
            ClaimRepository claims, RateRepository rates, SheetRepository sheets, Out<Rollup> before) {
        before.set(Rollup.of(sheetId.trim(), people, claims, rates, sheets));
    }
}
