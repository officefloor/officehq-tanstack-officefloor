package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds a person of the sheet to the crew: refused (nothing written) for no such crew or person, the
 *  crew's lead, or a person already in the crew. */
public class AddCrewMember {
    public void add(@PathVariable("sheetId") String sheetId, @PathVariable("crewId") long crewId,
            @RequestParam(name = "by", required = false) String by, @RequestBody CrewMemberRequest request,
            CrewRepository crews, PersonRepository people, Out<CrewMemberAdded> added)
            throws InvalidCrewException {
        String id = sheetId.trim();
        Crew crew = crews.find(id, crewId);
        if (crew == null) {
            throw new InvalidCrewException("No such crew: " + crewId);
        }
        if (request == null || request.personId() == null) {
            throw new InvalidCrewException("No member named");
        }
        Person person = people.find(id, request.personId());
        if (person == null) {
            throw new InvalidCrewException("No such person on the sheet: " + request.personId());
        }
        if (person.getId() == crew.leadId()) {
            throw new InvalidCrewException(person.getName() + " leads " + crew.name());
        }
        if (crew.members().stream().anyMatch(m -> m.personId() == person.getId())) {
            throw new InvalidCrewException(person.getName() + " is already in " + crew.name());
        }
        String user = by == null ? "" : by;
        crews.addMember(id, crewId, person.getId(), user);
        added.set(new CrewMemberAdded(id, crew.name(), person.getName(), user));
    }
}
