package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Flags the person with the reason until the until-date (the sheet's today when blank), replacing any flag they had.
 *  Only a steward (given the role or by delegation) may flag. Publishes the person as flagged and the flag stored. */
public class FlagPerson {
    public void flag(@Val Person person, @Val Flag flag, SheetRepository sheets, RoleRepository roles,
            PersonRepository people, Out<Person> flagged, Out<PersonFlag> stored) throws InvalidPersonException {
        if (!RoleRepository.STEWARD.equals(roles.roleOf(person.sheetId(), flag.by()))) {
            throw new InvalidPersonException("Only a steward may flag a person");
        }
        LocalDate until = flag.until() != null ? flag.until() : sheets.today(person.sheetId());
        PersonFlag set = new PersonFlag(flag.reason(), until);
        people.flag(person.sheetId(), person.id(), set);
        flagged.set(person.withFlag(set));
        stored.set(set);
    }
}
