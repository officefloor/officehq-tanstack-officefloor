package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** A name already on the sheet's crews, or a lead not on the sheet, is refused. */
public class SaveCrew {
    public void save(@Val CrewInput input, @Val Sheet sheet, CrewRepository crews, PersonRepository people,
            Out<Crew> crewOut) throws InvalidCrewException {
        if (crews.find(sheet.id(), input.name()) != null) {
            throw new InvalidCrewException(input.name() + " is already a crew");
        }
        Person lead = people.find(sheet.id(), input.leadId());
        if (lead == null) throw new InvalidCrewException("No such person to lead the crew");
        crewOut.set(crews.save(sheet.id(), input.name(), lead.getId()));
    }
}
