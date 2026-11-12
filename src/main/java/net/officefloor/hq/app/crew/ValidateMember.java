package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a member by a viewer, of a crew or person not on the sheet, of the crew's lead, or of a person
 *  already in the crew. */
public class ValidateMember {
    public void validate(@PathVariable("sheetId") String sheetId, @PathVariable("crewId") String crewId,
            @RequestBody MemberRequest request, Out<MemberInput> body, RoleStore roles, CrewStore crews,
            PersonStore people) throws InvalidSheetException {
        String sheet = sheetId.trim();
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheet, user);
        CrewStore.Crew crew;
        try {
            crew = crews.find(sheet, Long.parseLong(crewId.trim()));
        } catch (NumberFormatException e) {
            crew = null;
        }
        if (crew == null) {
            throw new InvalidSheetException("No such crew");
        }
        Person person = request == null || request.personId() == null ? null : people.find(sheet, request.personId());
        if (person == null) {
            throw new InvalidSheetException("A person is required");
        }
        if (person.getId() == crew.leadId()) {
            throw new InvalidSheetException(person.getName() + " leads " + crew.name());
        }
        if (crews.isMember(crew.id(), person.getId())) {
            throw new InvalidSheetException(person.getName() + " is already in " + crew.name());
        }
        body.set(new MemberInput(crew, person.getId(), person.getName(), user));
    }
}
