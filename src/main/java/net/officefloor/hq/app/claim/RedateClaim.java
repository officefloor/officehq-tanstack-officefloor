package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.role.StewardOnlyException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Re-dates an OPEN or PENDING claim (a steward). Its new date may not be after its person's
 * retirement. Its due-by and any figure a statement bound it to are kept, and so its period (the one
 * containing its due-by) and its number: a new date inside a closed period is allowed.
 */
public class RedateClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, @Val Redate redate, PersonRepository people,
            RoleRepository roles, HttpServletRequest request, Out<Redating> redatingOut)
            throws StewardOnlyException, InvalidClaimStatusException, RetiredPersonException {
        redatingOut.set(redate(sheet, claim, redate.date(), people, roles, Audit.user(request)));
    }

    /** Re-dates the claim by the rules above as the user, or refuses and leaves it as it is. */
    static Redating redate(Sheet sheet, Claim claim, LocalDate to, PersonRepository people, RoleRepository roles,
            String user) throws StewardOnlyException, InvalidClaimStatusException, RetiredPersonException {
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), user))) {
            throw new StewardOnlyException("Only a steward may re-date a claim");
        }
        if (!claim.isOpen()) {
            throw new InvalidClaimStatusException("A " + claim.getStatus() + " claim cannot be re-dated");
        }
        Person person = people.find(sheet.id(), claim.getPersonId());
        if (person != null && person.retiredBefore(to)) throw new RetiredPersonException(person.getName());

        LocalDate from = claim.getDate();
        claim.setDate(to);
        return new Redating(from, to);
    }
}
