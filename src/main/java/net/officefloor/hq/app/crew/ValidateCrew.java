package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

/** A crew's name (trimmed, required) and its lead (required). */
public class ValidateCrew {
    public void validate(@Val SheetResponse sheet, @RequestBody CrewRequest request, Out<Crew> crew,
            Out<ChangedBy> changedBy) throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("Enter a crew name");
        }
        if (name.length() > 200) {
            throw new BadRequestException("A crew name is at most 200 characters");
        }
        if (request.lead() == null) {
            throw new BadRequestException("Choose a lead");
        }
        crew.set(new Crew(sheet.id(), name, request.lead()));
        changedBy.set(by);
    }
}
