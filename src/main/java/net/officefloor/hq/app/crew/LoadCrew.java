package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Loads the sheet's crew by id; refuses one not on the sheet. */
public class LoadCrew {
    public void load(@PathVariable("crewId") long crewId, @Val Sheet sheet, CrewRepository repository,
            Out<Crew> crewOut) throws InvalidCrewException {
        crewOut.set(repository.findByIdAndSheetId(crewId, sheet.getId())
                .orElseThrow(() -> new InvalidCrewException("There is no such crew.")));
    }
}
