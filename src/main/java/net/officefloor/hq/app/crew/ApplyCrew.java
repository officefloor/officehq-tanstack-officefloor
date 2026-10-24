package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.pool.PoolRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the crew to the sheet with its lead; refuses a name already a crew's or a pool's, or a lead not on the sheet. */
public class ApplyCrew {
    public void apply(@Val CrewRequest request, @Val Sheet sheet, CrewRepository repository, PersonRepository people,
            PoolRepository pools, Out<Crew> crewOut, Out<CrewLead> leadOut) throws InvalidCrewException {
        if (repository.existsBySheetIdAndNameIgnoreCase(sheet.getId(), request.name())) {
            throw new InvalidCrewException("There is already a crew with that name.");
        }
        if (pools.existsBySheetIdAndNameIgnoreCase(sheet.getId(), request.name())) {
            throw new InvalidCrewException("There is already a pool with that name.");
        }
        Person lead = people.findByIdAndSheetId(request.leadId(), sheet.getId())
                .orElseThrow(() -> new InvalidCrewException("There is no such person on the sheet."));
        leadOut.set(new CrewLead(lead.getName()));
        crewOut.set(repository.saveAndFlush(new Crew(sheet.getId(), request.name(), lead.getId())));
    }

    /** The added crew's lead's name. */
    public record CrewLead(String name) {
    }
}
