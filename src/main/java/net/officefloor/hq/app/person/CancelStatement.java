package net.officefloor.hq.app.person;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** A steward cancels the person's last statement: the claims it bound (every claim of theirs bound
 *  now, as each statement rebinds or unbinds all of them) become live again, a figure bound by a
 *  statement before it not restored; the count goes down by one and they show the statement before
 *  it (or none). Refused for anyone but a steward, and for a person with no statement. */
public class CancelStatement {
    public void cancel(@RequestParam(name = "by", required = false) String by, @Val Person person,
            PersonRepository people, ClaimRepository claims, RoleRepository roles) throws InvalidPersonException {
        String sheetId = person.getSheetId();
        if (!Role.STEWARD.equals(roles.roleOf(sheetId, by == null ? "" : by))) {
            throw new InvalidPersonException("Only a steward can cancel a statement");
        }
        if (person.getStatementCount() <= 0) {
            throw new InvalidPersonException("The person has no statement to cancel");
        }
        for (Claim c : claims.listOf(sheetId, person.getId())) {
            if (c.isStated()) {
                c.bind(false, null);
                claims.updateBinding(c);
            }
        }
        people.cancelStatement(person);
    }
}
