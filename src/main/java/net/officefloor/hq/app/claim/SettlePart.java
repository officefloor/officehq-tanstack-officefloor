package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** After the split, before anything is saved: judges settling the split-off claim exactly as the settle
 *  button does (its four-eyes and proof rules already judged on the original), so a refusal writes
 *  nothing. The claim stays OPEN for the split's own save; the settle it would make is published. */
public class SettlePart {
    public void settle(@RequestParam(name = "by", required = false) String by, @Val Split split,
            PersonRepository people, RateRepository rates, PeriodRepository periods, RoleRepository roles,
            SettingRepository settings, SheetRepository sheets, Out<PartSettle> part)
            throws ClaimStatusException, PeriodClosedException {
        Claim created = split.created();
        SettleClaim.settle(by, created, false, people, rates, periods, roles, settings, sheets);
        part.set(new PartSettle(created.getStatus(), created.getSettledDue()));
        created.setStatus(ClaimStatus.OPEN.name());
        created.setSettledDue(null);
    }
}
