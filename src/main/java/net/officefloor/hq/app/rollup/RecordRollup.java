package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** After the change's own audit lines: a ROLLUP line per person whose figures moved, then TOTALS
 *  when a sheet total moved; nothing when no figure moved. */
public class RecordRollup {
    public void record(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, @Val Rollup before,
            PersonRepository people, ClaimRepository claims, RateRepository rates,
            SheetRepository sheets, Audit audit) {
        String id = sheetId.trim();
        Rollup after = Rollup.of(id, people, claims, rates, sheets);
        for (String line : after.changesSince(before, id, by == null ? "" : by)) {
            audit.record(line);
        }
    }
}
