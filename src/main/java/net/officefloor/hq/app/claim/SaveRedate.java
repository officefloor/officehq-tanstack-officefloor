package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.RetiredPersonException;
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
        saved.set(ClaimRedate.redate(input, loaded, repository, people, roles));
    }
}
