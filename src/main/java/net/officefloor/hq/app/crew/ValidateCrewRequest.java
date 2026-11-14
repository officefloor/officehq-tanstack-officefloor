package net.officefloor.hq.app.crew;

import jakarta.validation.Valid;
import java.util.ArrayList;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.pool.PoolRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses (400) a crew whose name is blank once trimmed or is already a crew's or a pool's on the sheet, or whose lead is not on the sheet. */
public class ValidateCrewRequest {
    public void validate(@Val Sheet sheet, @RequestBody @Valid CrewRequest request, CrewRepository crews, PoolRepository pools,
            PersonRepository people, Out<Crew> crew) throws InvalidCrewException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidCrewException("A crew's name is required.");
        }
        if (crews.nameTaken(sheet.id(), name)) {
            throw new InvalidCrewException("There is already a crew named " + name + ".");
        }
        if (pools.nameTaken(sheet.id(), name)) {
            throw new InvalidCrewException("There is already a pool named " + name + ", and a crew is a pool.");
        }
        Person lead = request.leadId() == null ? null : people.find(sheet.id(), request.leadId());
        if (lead == null) {
            throw new InvalidCrewException("Choose a lead on the sheet.");
        }
        crew.set(new Crew(null, sheet.id(), name, new CrewPerson(lead.id(), lead.name()), new ArrayList<>()));
    }
}
