package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.plugin.variable.Val;

/** Refuses a clerk settling a claim whose due is above the sheet's clerk limit. A claim whose due is
 *  n/a is within any limit; a sheet with no limit, and a steward, settle anything. */
public class CheckClerkLimit {
    public void check(@Val StatusInput input, @Val Claim claim, RoleStore roles, SettingStore settings,
            PersonStore people, RateStore rates) throws InvalidSheetException {
        if (input.status() != ClaimStatus.SETTLED || roles.find(claim.getSheetId(), input.user()) == Role.STEWARD) {
            return;
        }
        BigDecimal limit = settings.clerkLimit(claim.getSheetId());
        if (limit == null) {
            return;
        }
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        BigDecimal pct = person == null ? null : rates.find(claim.getSheetId(), person.getGrade(), claim.getDate());
        BigDecimal due = ClaimFigure.of(claim, pct, settings.floor(claim.getSheetId()));
        if (due != null && due.compareTo(limit) > 0) {
            throw new InvalidSheetException("A clerk can settle a claim only when its due is at or below "
                    + limit.toPlainString());
        }
    }
}
