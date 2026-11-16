package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Stores the crew on the sheet; an unknown lead, or a name already taken there, is refused. */
public class AddCrew {
    public void add(@PathVariable("sheetId") String sheetId, @Val CrewRequest request, CrewRepository crews,
            Out<CrewValues> values) throws InvalidCrewException {
        String sheet = sheetId.trim();
        String name = request.name().trim();
        String lead = crews.personName(sheet, request.leadId());
        if (lead == null) {
            throw new InvalidCrewException("No such person on the sheet");
        }
        if (!crews.add(sheet, name, request.leadId())) {
            throw new InvalidCrewException(name + " is already a crew");
        }
        values.set(new CrewValues(name, request.leadId(), lead, request.by() == null ? "" : request.by().trim()));
    }
}
