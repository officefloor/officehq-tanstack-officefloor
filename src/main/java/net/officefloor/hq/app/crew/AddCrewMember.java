package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds a person to a crew; refused for an unknown crew or person, the crew's lead, or a person already in the crew. */
public class AddCrewMember {
    public void add(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody CrewMemberRequest request, Crews crews, People people, Audit audit)
            throws InvalidCrewException {
        Long lead = request == null || request.crew() == null ? null : crews.leadOf(sheet, request.crew());
        if (lead == null) {
            throw new InvalidCrewException("No such crew");
        }
        Person person = request.person() == null ? null : people.find(sheet, request.person());
        if (person == null) {
            throw new InvalidCrewException("Choose a person");
        }
        if (person.id() == lead) {
            throw new InvalidCrewException(person.name() + " leads " + request.crew());
        }
        if (crews.isMember(sheet, request.crew(), person.id())) {
            throw new InvalidCrewException(person.name() + " is already in " + request.crew());
        }
        crews.addMember(sheet, request.crew(), person.id());
        audit.record("CREW_MEMBER_ADDED sheet=" + sheet + " by=" + user + " crew=" + request.crew() + " name="
                + person.name());
    }
}
