package net.officefloor.hq.app.claim;

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
        saved.set(ClaimReassign.move(input, loaded, repository, people, roles, settings, false));
    }
}
