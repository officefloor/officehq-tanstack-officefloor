package net.officefloor.hq.app.person;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** A steward flags the person with the reason until the date asked (the sheet's today when none),
 *  replacing any flag before. Refused for anyone but a steward. */
public class FlagPerson {
    public void flag(@RequestParam(name = "by", required = false) String by, @Val FlagRequest request,
            @Val Person person, SheetRepository sheets, RoleRepository roles, PersonRepository people)
            throws InvalidPersonException {
        if (!Role.STEWARD.equals(roles.roleOf(person.getSheetId(), by == null ? "" : by))) {
            throw new InvalidPersonException("Only a steward can flag a person");
        }
        String until = request.until() != null ? request.until() : sheets.today(person.getSheetId()).toString();
        person.setFlag(request.reason(), until);
        people.updateFlag(person);
    }
}
