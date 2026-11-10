package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonNotFoundException;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Moves a DRAFT, OPEN or PENDING claim to another person on the sheet. Only a role the permission matrix
 * allows (by the standing rule, a steward) may move it.
 * It keeps its number and date, loses any figure a statement bound it to, and is listed as its new
 * person's newest claim.
 */
public class ReassignClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, @Val ReassignRequest reassign, PersonRepository people,
            RoleRepository roles, SettingsRepository settings, HttpServletRequest request, Out<Reassignment> reassignmentOut)
            throws InvalidClaimStatusException, ApprovalException, PersonNotFoundException, InvalidClaimException {
        String user = Audit.user(request);
        String role = roles.roleOf(sheet.id(), user);
        if (!settings.find(sheet.id()).allows("reassign", role)) {
            throw new ApprovalException("A " + role + " may not move a claim: " + user + " is a " + role);
        }
        Person to = null;
        try {
            to = people.find(sheet.id(), Long.parseLong(reassign.personId()));
        } catch (NumberFormatException e) {
            // not an id: not found
        }
        if (to == null) throw new PersonNotFoundException(reassign.personId());
        if (to.getId() == claim.getPersonId()) {
            throw new InvalidClaimException("The claim is already " + to.getName() + "'s");
        }
        Person from = people.find(sheet.id(), claim.getPersonId());
        claim.reassign(to.getId());
        reassignmentOut.set(new Reassignment(from.getName(), to.getName()));
    }
}
