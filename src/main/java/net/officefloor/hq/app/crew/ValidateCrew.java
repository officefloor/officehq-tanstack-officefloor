package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateCrew {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody CrewRequest request,
            Out<ValidCrew> body, Settings settings, Crews crews, People people) throws InvalidCrewException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidCrewException("A crew name cannot be blank");
        }
        if (crews.contains(sheetId, name)) {
            throw new InvalidCrewException("There is already a crew of that name");
        }
        Person lead = ValidateCrewMember.person(sheetId, request.leadId(), people);
        body.set(new ValidCrew(name, lead.getId(), lead.getName(), request.user()));
    }
}
