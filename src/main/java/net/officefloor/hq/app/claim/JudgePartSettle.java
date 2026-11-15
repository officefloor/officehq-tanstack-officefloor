package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** First step of a part-settle, before the split: the settle's role, four-eyes and proof rules judged
 *  on the original claim as it stands (its own amount, opener, editor and proofs). */
public class JudgePartSettle {
    public void judge(@RequestParam(name = "by", required = false) String by, @Val Claim claim,
            PersonRepository people, RateRepository rates, RoleRepository roles, SettingRepository settings,
            SheetRepository sheets) throws ClaimStatusException {
        if (!Permissions.allows(claim.getSheetId(), by, Permissions.SETTLE, roles, settings)) {
            throw new ClaimStatusException("Your role may not settle a claim");
        }
        SettleClaim.judge(by, claim, people, rates, settings, sheets);
    }
}
