package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the person to the crew. The crew's lead, or a person already in the crew, is refused. */
public class AddCrewMember {
    public void add(@Val SheetResponse sheet, @Val CrewMemberInput input, CrewRepository crews, PersonRepository people,
            Out<CrewMemberAdded> added) throws BadRequestException {
        Long crewId = crews.findId(sheet.id(), input.crew());
        if (crewId == null) {
            throw new BadRequestException("No such crew on this sheet");
        }
        Person person = people.find(sheet.id(), input.personId());
        if (person == null) {
            throw new BadRequestException("No such person on this sheet");
        }
        if (Long.valueOf(person.getId()).equals(crews.leadOf(crewId))) {
            throw new BadRequestException(person.getName() + " leads this crew");
        }
        if (!crews.addMember(sheet.id(), crewId, person.getId())) {
            throw new BadRequestException(person.getName() + " is already in this crew");
        }
        added.set(new CrewMemberAdded(sheet.id(), input.crew(), person.getName()));
    }
}
