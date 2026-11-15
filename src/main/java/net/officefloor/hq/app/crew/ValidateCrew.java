package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refused for a blank name, one already used by a crew of the sheet (compared trimmed), and no lead. */
public class ValidateCrew {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody CrewRequest request,
            CrewRepository crews, Out<CrewRequest> body) throws InvalidCrewException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidCrewException("A crew needs a name");
        }
        if (crews.contains(sheetId.trim(), name)) {
            throw new InvalidCrewException("Already a crew: " + name);
        }
        if (request.leadId() == null) {
            throw new InvalidCrewException("A crew needs a lead");
        }
        body.set(new CrewRequest(name, request.leadId()));
    }
}
