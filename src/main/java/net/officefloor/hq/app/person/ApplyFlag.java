package net.officefloor.hq.app.person;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Flags the person with the reason until the until-date (the sheet's today when blank), replacing any flag they
 *  had. Only a steward may. */
public class ApplyFlag {
    public void apply(@Val FlagInput input, @Val Sheet sheet, @Val Person person, PersonRepository repository,
            RoleRepository roles, @RequestHeader(name = "X-User", required = false) String user)
            throws InvalidPersonException {
        if (!Role.STEWARD.equals(roles.roleOf(sheet.getId(), AuditUser.decode(user)))) {
            throw new InvalidPersonException("Only a steward can flag a person.");
        }
        person.flag(input.reason(), input.until() == null ? sheet.getToday() : input.until());
        repository.saveAndFlush(person);
    }
}
