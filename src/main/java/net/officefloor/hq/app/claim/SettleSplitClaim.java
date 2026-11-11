package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Settles the claim a part-settle split off, as the settle button would (to PENDING when the clerk
 * limit says so), the four-eyes and proof rules having been judged on the original; saves it and
 * logs its status. Keeps the sheet's figures as split, so the rollup lines tell what the settle moved.
 */
public class SettleSplitClaim {
    public void settle(@Val Sheet sheet, @Val Split split, PersonRepository people, RateRepository rates,
            RoleRepository roles, SettingsRepository settings, ClaimRepository claims, PeriodRepository periods, ClaimLogRepository log,
            HttpServletRequest request, Out<PartSettle> partOut)
            throws InvalidClaimStatusException, SelfSettleException {
        partOut.set(new PartSettle(Rollup.of(sheet.id(), sheet.today(), people, claims, rates, settings.find(sheet.id()))));
        Claim made = split.made();
        String user = Audit.user(request);
        SettleClaim.settle(sheet, made, user, people, rates, roles, settings, true);
        made.setSettleBatch(null);
        ClaimNumbers.giveOnSettle(sheet, made, settings.find(sheet.id()), periods, claims);
        claims.save(made);
        log.record(made, user, sheet.today(), LogClaimStatus.due(sheet, made, people, rates, settings));
    }
}
