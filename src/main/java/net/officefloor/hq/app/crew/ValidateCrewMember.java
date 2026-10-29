package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateCrewMember {

    // The crew and the person must be of the sheet; the person is not the crew's lead, nor already in the crew.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody CrewMemberRequest body, Crews crews,
            People people, Out<CrewMember> member) throws CrewRefusedException {
        String sheet = sheetId.trim();
        String crew = body == null || body.crew() == null ? "" : body.crew().trim();
        Long lead = crews.lead(sheet, crew);
        if (lead == null) {
            throw new CrewRefusedException("Choose a crew.");
        }
        Person person = body.personId() == null ? null : people.find(sheet, body.personId());
        if (person == null) {
            throw new CrewRefusedException("Choose a person.");
        }
        if (person.id() == lead) {
            throw new CrewRefusedException(person.name() + " leads the crew.");
        }
        if (crews.isMember(sheet, crew, person.id())) {
            throw new CrewRefusedException(person.name() + " is already in the crew.");
        }
        member.set(new CrewMember(sheet, crew, person.id(), person.name()));
    }
}
