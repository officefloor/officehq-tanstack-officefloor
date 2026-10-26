package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.pool.PoolRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the crew; a name already on the sheet, as a crew or a pool, or a lead not on it, is refused. */
public class BuildCrew {
    public void build(@Val Crew crew, CrewRepository repository, PersonRepository people,
            PoolRepository pools, Out<CrewAdded> added)
            throws BadRequestException {
        Person lead = people.find(crew.sheetId(), crew.leadId());
        if (lead == null) {
            throw new BadRequestException("No such person on this sheet");
        }
        if (repository.findId(crew.sheetId(), crew.name()) != null) {
            throw new BadRequestException("That crew is already on this sheet");
        }
        if (pools.findId(crew.sheetId(), crew.name()) != null) {
            throw new BadRequestException("A pool of that name is already on this sheet");
        }
        repository.insert(crew.sheetId(), crew.name(), lead.getId());
        added.set(new CrewAdded(crew.sheetId(), crew.name(), lead.getName()));
    }
}
