package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonNotFoundException;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Move the loaded claim to another person on the sheet. Only a role the sheet's permission matrix lets reassign
 *  may (by default a steward), and only a DRAFT, OPEN or PENDING
 *  claim that is not a correction (a correction names a claim of its own person), to another person it may be
 *  dated for (not retired before its date). It keeps its number and date, and loses any figure a statement bound
 *  it to: its due is live again until its new person's next statement. A move another user's change got to first
 *  is refused. */
public class SaveReassign {
    public void save(@Val ReassignInput input, @Val Claim loaded, ClaimRepository repository,
            PersonRepository people, RoleRepository roles, SettingRepository settings, Out<ClaimResponse> saved)
            throws ClaimStatusNotAllowedException, PersonNotFoundException, RetiredPersonException {
        if (!settings.permissions(loaded.sheetId()).allows("reassign", roles.of(loaded.sheetId(), input.user()))) {
            throw new ClaimStatusNotAllowedException("Your role may not move a claim to another person on this sheet.");
        }
        if (!loaded.status().unsettled() && loaded.status() != ClaimStatus.DRAFT) {
            throw new ClaimStatusNotAllowedException("A " + loaded.status()
                    + " claim cannot move; only a DRAFT, OPEN or PENDING one can.");
        }
        if (loaded.corrects() != null) {
            throw new ClaimStatusNotAllowedException("A correction cannot move to another person.");
        }
        Person to = people.find(loaded.sheetId(), input.personId());
        if (to == null) {
            throw new PersonNotFoundException(String.valueOf(input.personId()));
        }
        if (to.id() == loaded.personId()) {
            throw new ClaimStatusNotAllowedException("The claim is already " + to.name() + "'s.");
        }
        RetiredPersonException.refuse(to, loaded.date());
        if (!repository.reassign(loaded, to.id())) {
            throw new ClaimStatusNotAllowedException("The claim changed meanwhile; load the sheet again.");
        }
        saved.set(ClaimResponse.from(new Claim(loaded.id(), loaded.sheetId(), to.id(), loaded.amount(),
                loaded.date(), loaded.note(), loaded.status(), loaded.number(), loaded.settledDue(),
                loaded.openedBy(), false, null, loaded.corrects(), loaded.dueBy(), loaded.deferredUntil(),
                loaded.proofs(), loaded.splitFrom(), loaded.lateEntry(), loaded.reminded(), loaded.ref())));
    }
}
