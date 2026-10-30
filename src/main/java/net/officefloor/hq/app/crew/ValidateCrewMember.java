package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateCrewMember {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody CrewMemberRequest request,
            Out<ValidCrewMember> body, Settings settings, Crews crews, People people)
            throws InvalidCrewException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String crew = request.crew() == null ? "" : request.crew();
        if (!crews.contains(sheetId, crew)) {
            throw new InvalidCrewException("No such crew");
        }
        Person person = person(sheetId, request.personId(), people);
        if (crews.leadOf(sheetId, crew) == person.getId()) {
            throw new InvalidCrewException("The person leads the crew");
        }
        if (crews.hasMember(sheetId, crew, person.getId())) {
            throw new InvalidCrewException("The person is already in the crew");
        }
        body.set(new ValidCrewMember(crew, person.getId(), person.getName(), request.user()));
    }

    /** The sheet's person of the id. */
    static Person person(String sheetId, String rawId, People people) throws InvalidCrewException {
        long id;
        try {
            id = Long.parseLong(rawId == null ? "" : rawId.trim());
        } catch (NumberFormatException e) {
            throw new InvalidCrewException("Choose a person");
        }
        Person person = people.find(sheetId, id);
        if (person == null) {
            throw new InvalidCrewException("No such person");
        }
        return person;
    }
}
