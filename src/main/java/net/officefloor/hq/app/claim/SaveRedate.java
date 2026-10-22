package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Re-date the loaded claim: only a steward may, and only an OPEN or PENDING claim, to a date its person may claim
 *  for (not after their retirement). Its due-by, and so its period and number, and any figure a statement bound
 *  it to stay as they are. A re-date another user's change got to first is refused. */
public class SaveRedate {
    public void save(@Val RedateInput input, @Val Claim loaded, ClaimRepository repository, PersonRepository people,
            RoleRepository roles, Out<ClaimResponse> saved)
            throws ClaimStatusNotAllowedException, RetiredPersonException {
        if (!Role.STEWARD.equals(roles.of(loaded.sheetId(), input.user()))) {
            throw new ClaimStatusNotAllowedException("Only a steward may re-date a claim.");
        }
        if (!loaded.status().unsettled()) {
            throw new ClaimStatusNotAllowedException("A " + loaded.status()
                    + " claim cannot be re-dated; only an OPEN or PENDING one can.");
        }
        RetiredPersonException.refuse(people.find(loaded.sheetId(), loaded.personId()), input.date());
        String number = loaded.number();
        if (!repository.redate(loaded, input.date(), number)) {
            throw new ClaimStatusNotAllowedException("The claim changed meanwhile; load the sheet again.");
        }
        saved.set(ClaimResponse.from(new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), loaded.amount(),
                input.date(), loaded.note(), loaded.status(), number, loaded.settledDue(), loaded.openedBy(),
                loaded.stated(), loaded.statedDue(), loaded.corrects(), loaded.dueBy(), loaded.deferredUntil(),
                loaded.proofs(), loaded.splitFrom(), loaded.lateEntry(), loaded.reminded(), loaded.ref(), loaded.tags())));
    }
}
