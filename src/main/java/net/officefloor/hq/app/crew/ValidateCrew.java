package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.pool.Pools;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateCrew {

    // The name, trimmed, must not be blank, nor already a crew or a pool of the sheet; the lead must be a person of the sheet.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody CrewRequest body, Crews crews,
            People people, Pools pools, Out<Crew> crew) throws CrewRefusedException {
        String sheet = sheetId.trim();
        String name = body == null || body.name() == null ? "" : body.name().trim();
        if (name.isEmpty()) {
            throw new CrewRefusedException("Give the crew a name.");
        }
        if (crews.has(sheet, name)) {
            throw new CrewRefusedException("\"" + name + "\" is already a crew.");
        }
        if (pools.has(sheet, name)) {
            throw new CrewRefusedException("\"" + name + "\" is already a pool.");
        }
        Person lead = body.leadId() == null ? null : people.find(sheet, body.leadId());
        if (lead == null) {
            throw new CrewRefusedException("Choose the crew's lead.");
        }
        crew.set(new Crew(sheet, name, lead.id(), lead.name()));
    }
}
