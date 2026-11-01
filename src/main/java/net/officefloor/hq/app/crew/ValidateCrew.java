package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a crew name that is blank, or already a crew on the sheet (its name trimmed), or a lead that
 *  is not a person of the sheet, or a name already a pool on the sheet (a crew is a pool too). */
public class ValidateCrew {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody CrewRequest request,
            Crews crews, Out<Crew> crew) throws InvalidSheetRequestException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidSheetRequestException("A crew's name cannot be blank");
        }
        if (name.length() > 200) {
            throw new InvalidSheetRequestException("A crew's name is at most 200 characters");
        }
        if (crews.has(sheetId, name)) {
            throw new InvalidSheetRequestException("Already a crew: " + name);
        }
        if (crews.hasPool(sheetId, name)) {
            throw new InvalidSheetRequestException("Already a pool: " + name);
        }
        String lead = request.leadId() == null ? null : crews.personName(sheetId, request.leadId());
        if (lead == null) {
            throw new InvalidSheetRequestException("No such person to lead the crew");
        }
        crew.set(new Crew(sheetId, name, request.leadId(), lead, request.user()));
    }
}
