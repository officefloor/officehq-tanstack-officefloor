package net.officefloor.hq.app.flag;

import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Flag the loaded person, replacing any flag they had. Refused unless the user acts as a steward on the sheet. */
public class SaveFlag {
    public void save(@Val FlagInput input, @Val Person loaded, RoleRepository roles, FlagRepository flags,
            Out<FlagResponse> saved) throws InvalidPersonException {
        if (!Role.STEWARD.equals(roles.of(loaded.sheetId(), input.user()))) {
            throw new InvalidPersonException("Only a steward can flag a person.");
        }
        flags.flag(loaded.sheetId(), loaded.id(), input.reason(), input.until());
        saved.set(new FlagResponse(loaded.id(), input.reason(), input.until().toString()));
    }
}
