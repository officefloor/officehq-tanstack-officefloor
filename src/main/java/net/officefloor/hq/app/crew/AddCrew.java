package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds the crew with its lead: refused (nothing written) for a lead not on the sheet. */
public class AddCrew {
    public void add(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @Val CrewRequest request, SheetRepository sheets, PersonRepository people, CrewRepository crews,
            Out<CrewAdded> added) throws InvalidCrewException {
        String id = sheetId.trim();
        Person lead = people.find(id, request.leadId());
        if (lead == null) {
            throw new InvalidCrewException("No such person on the sheet: " + request.leadId());
        }
        String user = by == null ? "" : by;
        sheets.open(id, user);
        crews.add(id, request.name(), lead.getId(), user);
        added.set(new CrewAdded(id, request.name(), lead.getName(), user));
    }
}
