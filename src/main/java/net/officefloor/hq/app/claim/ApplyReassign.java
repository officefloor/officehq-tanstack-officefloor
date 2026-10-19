package net.officefloor.hq.app.claim;

import java.util.Set;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Moves the claim to another person on the sheet. Only a steward moves a claim, and only a DRAFT, OPEN or
 *  PENDING one. The claim keeps its number and date but loses any figure a statement bound it to: it follows
 *  its live figure at its new person's grade until that person's next statement. Publishes both names. */
public class ApplyReassign {
    private static final Set<ClaimStatus> MOVABLE = Set.of(ClaimStatus.DRAFT, ClaimStatus.OPEN, ClaimStatus.PENDING);

    public void apply(@Val ReassignRequest request, @Val Claim claim, PersonRepository people, RoleRepository roles,
            Out<Reassignment> reassignmentOut, @RequestParam(value = "user", required = false) String user)
            throws InvalidClaimStatusException {
        if (!Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), user))) {
            throw new InvalidClaimStatusException("Only a steward can move a claim to another person.");
        }
        if (!MOVABLE.contains(claim.getStatus())) {
            throw new InvalidClaimStatusException("A " + claim.getStatus() + " claim cannot be moved to another person.");
        }
        Person from = people.findById(claim.getSheetId(), claim.getPersonId());
        Person to = people.findById(claim.getSheetId(), request.personId());
        if (to == null) {
            throw new InvalidClaimStatusException("There is no such person on the sheet.");
        }
        if (to.getId() == from.getId()) {
            throw new InvalidClaimStatusException("The claim is already " + to.getName() + "'s.");
        }
        claim.setPersonId(to.getId());
        claim.setStated(false);
        claim.setStatedDue(null);
        reassignmentOut.set(new Reassignment(from.getName(), to.getName()));
    }
}
