package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Refuse a part-settle, before the split is stored, when the user's settle of the loaded claim would be refused by
 *  the settle button's rules ({@link ClaimStatusChange}): the user who opened or last edited it, a deferred claim, a
 *  due above the proof threshold without its proofs, a role that may not settle. Writes nothing. */
public class RefuseUnsettleablePart {
    public void refuse(@Val SplitInput input, @Val Claim loaded, ClaimRepository repository, PersonRepository people,
            RateRepository rates, RoleRepository roles, SettingRepository settings, SheetRepository sheets,
            ClaimLogRepository log) throws ClaimStatusNotAllowedException {
        ClaimStatusChange.check(new ClaimStatusInput(input.user(), "settle", ClaimStatus.SETTLED, null), loaded,
                repository, people, rates, roles, settings, sheets, log);
    }
}
