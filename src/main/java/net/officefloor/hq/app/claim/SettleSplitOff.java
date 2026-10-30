package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Watches;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/**
 * Settles the claim a part-settle split off, by the user, as the settle button does (PENDING above the clerk limit),
 * its four-eyes and proof rules having been judged on the original claim. A refusal rolls back the whole part-settle.
 */
public class SettleSplitOff {
    public void settle(@Val ValidSplit request, @Val ClaimSplit split, Claims claims, Sheets sheets, People people,
            Rates rates, Periods periods, Settings settings, ClaimNumbering numbering, ClaimLog log, Audit audit,
            Rollup rollup, Watches watches) throws ClaimStatusRefusedException {
        Claim into = split.into();
        RollupFigures before = rollup.figures(into.getSheetId());
        ValidClaimStatus settle = new ValidClaimStatus(ClaimStatus.SETTLED, request.user(),
                ValidClaimStatus.Decision.NONE, null);
        ClaimNumber number = ApplyClaimStatus.change(settle, into, sheets, people, rates, periods, settings, numbering,
                log, false);
        claims.save(into);
        AuditClaimStatus.record(settle, into, number, people, audit, before, rollup, log, watches);
    }
}
