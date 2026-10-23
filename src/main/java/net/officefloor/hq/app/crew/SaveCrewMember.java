package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Store the member, refusing an unknown crew or person, the crew's lead, or a person already in the crew. */
public class SaveCrewMember {
    public void save(@PathVariable("sheetId") String sheetId, @PathVariable("crewId") String crewId,
            @Val CrewMemberInput input, PeriodRepository periods, CrewRepository repository, PersonRepository people,
            Out<CrewMember> member) throws InvalidCrewException {
        String id = sheetId.trim();
        periods.lockSheet(id);
        Crew crew;
        try {
            crew = repository.find(id, Long.parseLong(crewId.trim()));
        } catch (NumberFormatException e) {
            crew = null;
        }
        if (crew == null) {
            throw new InvalidCrewException("No crew " + crewId + " on this sheet.");
        }
        Person person = people.find(id, input.personId());
        if (person == null) {
            throw new InvalidCrewException("No person " + input.personId() + " on this sheet.");
        }
        if (crew.leadId() == person.id()) {
            throw new InvalidCrewException(person.name() + " already leads " + crew.name() + ".");
        }
        if (repository.isMember(crew, person.id())) {
            throw new InvalidCrewException(person.name() + " is already in " + crew.name() + ".");
        }
        repository.addMember(crew, person.id(), input.user());
        member.set(new CrewMember(crew.id(), crew.sheetId(), crew.name(), person.id(), person.name(), input.user()));
    }
}
