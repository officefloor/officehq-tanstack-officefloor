package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Re-settles a SETTLED claim; only a steward re-settles, and not a claim dated inside a closed period.
 * The settled due is frozen again at the figure the claim would have today as an open claim bound by no
 * statement: its amount (raised to the floor) at the rate of its person's grade in force on its date,
 * with its late charge as of today on top.
 */
public class ApplyResettle {
    public void apply(@Val ResettleRequest request, @Val Claim claim, Sheets sheets, People people, Rates rates,
            Periods periods, Settings settings, Out<ClaimResettle> resettle) throws ClaimStatusRefusedException {
        String sheetId = claim.getSheetId();
        if (!Settings.STEWARD.equals(settings.role(sheetId, request.user()))) {
            throw new ClaimStatusRefusedException("Only a steward can re-settle a claim");
        }
        if (claim.getStatus() != ClaimStatus.SETTLED) {
            throw new ClaimStatusRefusedException("Only a SETTLED claim can be re-settled");
        }
        if (periods.isClosed(sheetId, claim.getDate())) {
            throw new ClaimStatusRefusedException("No claim dated inside a closed period can be re-settled");
        }
        RoundingMode rounding = Rounding.of(claim, settings.policyDate(sheetId));
        String grade = people.find(sheetId, claim.getPersonId()).gradeOn(claim.getDate());
        BigDecimal figure = Rates.due(claim.getAmount(), rates.find(sheetId, grade, claim.getDate()),
                settings.floor(sheetId, claim.getDate()), rounding);
        BigDecimal due = figure == null ? null
                : figure.add(LateCharge.asOpen(claim, figure, settings.latePct(sheetId, claim.getDate()), sheets.today(sheetId), rounding));
        BigDecimal old = claim.getSettledDue();
        claim.setSettledDue(due);
        resettle.set(new ClaimResettle(old, due));
    }
}
