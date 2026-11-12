package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a crew by a viewer, a blank name, a name already a crew on the sheet, or a lead not on the sheet. */
public class ValidateCrew {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody CrewRequest request,
            Out<CrewInput> body, RoleStore roles, CrewStore crews, PersonStore people) throws InvalidSheetException {
        String sheet = sheetId.trim();
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheet, user);
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidSheetException("A crew name is required");
        }
        if (crews.has(sheet, name)) {
            throw new InvalidSheetException(name + " is already a crew");
        }
        Person lead = request.leadId() == null ? null : people.find(sheet, request.leadId());
        if (lead == null) {
            throw new InvalidSheetException("A lead is required");
        }
        body.set(new CrewInput(name, lead.getId(), lead.getName(), user));
    }
}
