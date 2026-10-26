package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.Role;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** Flags the person with the reason until the date; only a steward may. */
public class FlagPerson {
    public void flag(@Val FlagInput input, @Val Person person, @Val ChangedBy by, RoleRepository roles,
            PersonRepository repository) throws BadRequestException {
        if (!Role.STEWARD.equals(roles.roleOf(person.getSheetId(), by.name()))) {
            throw new BadRequestException("Only a steward may flag a person");
        }
        repository.flag(person.getId(), input.reason(), input.until());
        person.setFlagReason(input.reason());
        person.setFlagUntil(input.until());
    }
}
