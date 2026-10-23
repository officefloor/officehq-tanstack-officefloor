package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.pool.PoolRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Store the crew, refusing a name already a crew or a pool on the sheet, or an unknown lead. */
public class SaveCrew {
    public void save(@PathVariable("sheetId") String sheetId, @Val CrewInput input, PeriodRepository periods,
            CrewRepository repository, PoolRepository pools, PersonRepository people, Out<CrewAdded> crew) throws InvalidCrewException {
        String id = sheetId.trim();
        periods.lockSheet(id);
        if (repository.find(id, input.name()) != null) {
            throw new InvalidCrewException(input.name() + " is already a crew.");
        }
        if (pools.find(id, input.name()) != null) {
            throw new InvalidCrewException(input.name() + " is already a pool.");
        }
        Person lead = people.find(id, input.leadId());
        if (lead == null) {
            throw new InvalidCrewException("No person " + input.leadId() + " on this sheet.");
        }
        crew.set(new CrewAdded(repository.add(id, input), lead.name()));
    }
}
