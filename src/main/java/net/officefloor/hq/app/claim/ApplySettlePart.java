package net.officefloor.hq.app.claim;

import java.math.RoundingMode;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/**
 * Part-settles an OPEN claim: the settle's four-eyes and proof rules are judged on the original claim, as it is
 * before the split, and the user's role must be one that may settle; then the amount is split off as the split
 * button does. The new claim is settled by {@link SettleSplitOff}.
 */
public class ApplySettlePart {
    public void apply(@Val ValidSplit request, @Val Claim claim, Sheets sheets, People people, Rates rates,
            Periods periods, Settings settings) throws ClaimStatusRefusedException {
        if (!settings.allows(claim.getSheetId(), request.user(), "settle")) {
            throw new ClaimStatusRefusedException("Your role may not settle a claim on this sheet");
        }
        RoundingMode rounding = Rounding.of(claim, settings.policyDate(claim.getSheetId()),
                on -> settings.roundingMode(claim.getSheetId(), on));
        ApplyClaimStatus.refuseSettleRules(claim, request.user(), sheets, people, rates, settings, rounding);
        new ApplySplit().apply(request, claim, sheets, periods, settings);
    }
}
