package net.officefloor.hq.app.person;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.role.StewardOnlyException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Flags the person (a steward) with a reason until the date given, or the sheet's today when none is. */
public class FlagPerson {
    public void flag(@Val FlagInput input, @Val Sheet sheet, @Val Person person, PersonRepository repository,
            RoleRepository roles, HttpServletRequest request) throws StewardOnlyException {
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), Audit.user(request)))) {
            throw new StewardOnlyException("Only a steward may flag a person");
        }
        person.setFlag(input.reason(), input.until() != null ? input.until() : sheet.today());
        repository.saveFlag(person);
    }
}
