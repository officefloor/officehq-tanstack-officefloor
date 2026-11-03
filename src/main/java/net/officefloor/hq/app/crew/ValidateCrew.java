package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the crew form: a name is required (surrounding spaces removed) and a lead must be chosen. */
public class ValidateCrew {
    public void validate(@RequestBody CrewRequest request, Out<CrewRequest> crew) throws BadRequestException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("name is required");
        }
        if (request.leadId() == null) {
            throw new BadRequestException("choose a lead");
        }
        crew.set(new CrewRequest(name, request.leadId()));
    }
}
