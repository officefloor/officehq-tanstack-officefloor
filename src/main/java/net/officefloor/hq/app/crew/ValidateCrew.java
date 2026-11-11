package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A crew needs a name, kept trimmed, and a lead. */
public class ValidateCrew {
    public void validate(@RequestBody CrewRequest request, Out<CrewInput> body) throws InvalidCrewException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) throw new InvalidCrewException("A crew name is required");
        if (name.length() > 200) throw new InvalidCrewException("A crew name is at most 200 characters");
        body.set(new CrewInput(name, id(request.leadId(), "A lead is required")));
    }

    static Long id(String value, String message) throws InvalidCrewException {
        try {
            return Long.parseLong(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            throw new InvalidCrewException(message);
        }
    }
}
