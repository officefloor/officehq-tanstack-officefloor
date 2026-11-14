package net.officefloor.hq.app.crew;

import jakarta.validation.Valid;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses (400) a member for a crew not on the sheet, a person not on the sheet, the crew's lead, or one already in it. */
public class ValidateCrewMemberRequest {
    public void validate(@Val Sheet sheet, @PathVariable("crewId") String crewId,
            @RequestBody @Valid CrewMemberRequest request, CrewRepository crews, PersonRepository people,
            Out<NewCrewMember> added) throws InvalidCrewException {
        Crew crew;
        try {
            crew = crews.find(sheet.id(), Long.parseLong(crewId));
        } catch (NumberFormatException e) {
            crew = null;
        }
        if (crew == null) {
            throw new InvalidCrewException("That crew is not on the sheet.");
        }
        Person person = request == null || request.personId() == null ? null
                : people.find(sheet.id(), request.personId());
        if (person == null) {
            throw new InvalidCrewException("Choose a person on the sheet.");
        }
        if (crew.lead() != null && crew.lead().id() == person.id()) {
            throw new InvalidCrewException(person.name() + " leads the crew.");
        }
        if (crew.members().stream().anyMatch(m -> m.id() == person.id())) {
            throw new InvalidCrewException(person.name() + " is already in the crew.");
        }
        added.set(new NewCrewMember(crew, new CrewPerson(person.id(), person.name())));
    }
}
