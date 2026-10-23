package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Refuse a part-settle, before the split is stored, when the user's settle of the loaded claim would be refused by
 *  the settle button's rules ({@link ClaimStatusChange}): the user who opened or last edited it, a deferred claim, a
 *  due above the proof threshold without its proofs, a role that may not settle; or when the settle of the part split off (opened by the user) would be refused by them.
 *  Writes nothing. */
public class RefuseUnsettleablePart {
    public void refuse(@Val SplitInput input, @Val Claim loaded, ClaimRepository repository, PersonRepository people,
            RateRepository rates, RoleRepository roles, SettingRepository settings, SheetRepository sheets,
            ClaimLogRepository log) throws ClaimStatusNotAllowedException {
        ClaimStatusChange.check(new ClaimStatusInput(input.user(), "settle", ClaimStatus.SETTLED, null), loaded,
                repository, people, rates, roles, settings, sheets, log);
        // the part split off is opened by the user, and the screen then settles it as the settle button does: refuse
        // here when that settle would be refused, so a refused part-settle writes nothing
        Claim part = new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), input.amount(), loaded.date(),
                loaded.note(), ClaimStatus.OPEN, loaded.number(), null, input.user(), false, null, loaded.corrects(),
                loaded.dueBy(), null, 0, loaded.number(), loaded.lateEntry(), null, loaded.ref(), loaded.tags(), false, 0,
                loaded.held());
        ClaimStatusChange.check(new ClaimStatusInput(input.user(), "settle", ClaimStatus.SETTLED, null), part,
                repository, people, rates, roles, settings, sheets, log);
    }
}
