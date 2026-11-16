package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Stores the member in the crew; an unknown crew or person, the crew's lead, or a person already in the crew is
 *  refused. */
public class AddCrewMember {
    public void add(@PathVariable("sheetId") String sheetId, @Val CrewMemberRequest request, CrewRepository crews,
            Out<CrewMemberValues> values) throws InvalidCrewException {
        String sheet = sheetId.trim();
        String crew = request.crew().trim();
        Long crewId = crews.idOf(sheet, crew);
        if (crewId == null) {
            throw new InvalidCrewException("No crew " + crew);
        }
        String name = crews.personName(sheet, request.personId());
        if (name == null) {
            throw new InvalidCrewException("No such person on the sheet");
        }
        if (java.util.Objects.equals(crews.leadOf(crewId), request.personId())) {
            throw new InvalidCrewException(name + " leads " + crew);
        }
        if (!crews.addMember(crewId, request.personId())) {
            throw new InvalidCrewException(name + " is already in " + crew);
        }
        values.set(new CrewMemberValues(crew, request.personId(), name,
                request.by() == null ? "" : request.by().trim()));
    }
}
