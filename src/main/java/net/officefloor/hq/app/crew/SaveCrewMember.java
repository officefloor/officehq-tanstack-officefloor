package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** A crew or person not on the sheet, the crew's lead, or a person already in the crew, is refused. */
public class SaveCrewMember {
    public void save(@Val CrewMemberInput input, @Val Sheet sheet, CrewRepository crews, PersonRepository people,
            Out<CrewMember> memberOut) throws InvalidCrewException {
        Crew crew = crews.find(sheet.id(), input.crew());
        if (crew == null) throw new InvalidCrewException("No crew " + input.crew());
        Person person = people.find(sheet.id(), input.personId());
        if (person == null) throw new InvalidCrewException("No such person");
        if (person.getId().equals(crew.leadId())) {
            throw new InvalidCrewException(person.getName() + " leads " + crew.name());
        }
        if (crews.isMember(crew, person.getId())) {
            throw new InvalidCrewException(person.getName() + " is already in " + crew.name());
        }
        crews.addMember(crew, person.getId());
        memberOut.set(new CrewMember(sheet.id(), crew.name(), person.getId(), person.getName()));
    }
}
