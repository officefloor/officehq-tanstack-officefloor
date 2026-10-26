package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the crew; a name already on the sheet, or a lead not on it, is refused. */
public class BuildCrew {
    public void build(@Val Crew crew, CrewRepository repository, PersonRepository people, Out<CrewAdded> added)
            throws BadRequestException {
        Person lead = people.find(crew.sheetId(), crew.leadId());
        if (lead == null) {
            throw new BadRequestException("No such person on this sheet");
        }
        if (repository.findId(crew.sheetId(), crew.name()) != null) {
            throw new BadRequestException("That crew is already on this sheet");
        }
        repository.insert(crew.sheetId(), crew.name(), lead.getId());
        added.set(new CrewAdded(crew.sheetId(), crew.name(), lead.getName()));
    }
}
