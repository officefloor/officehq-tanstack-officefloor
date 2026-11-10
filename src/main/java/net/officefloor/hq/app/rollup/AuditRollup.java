package net.officefloor.hq.app.rollup;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Records, after the change's own lines, the figures it moved: a ROLLUP line per person whose
 * claimed or due-total changed, then a TOTALS line when the sheet's totals changed. The sheet's
 * today is read again, as the change may have moved it.
 */
public class AuditRollup {
    public void audit(@Val Sheet sheet, @Val Rollup before, SheetRepository sheets, PersonRepository people,
            ClaimRepository claims, RateRepository rates, HttpServletRequest request, Audit audit) {
        Rollup after = Rollup.of(sheet.id(), sheets.today(sheet.id()), people, claims, rates);
        after.changesSince(before, sheet.id(), Audit.user(request)).forEach(audit::record);
    }
}
