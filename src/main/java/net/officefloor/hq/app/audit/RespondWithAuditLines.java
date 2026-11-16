package net.officefloor.hq.app.audit;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's audit lines exactly as the file holds them, newest first; with a name, only those about that person
 *  (see Audit#lines). Read from the file: the viewer is not a second record. When the name is of a person transferred
 *  here, the lines about them on the sheet they came from follow, newest first, from the transfer that brought them
 *  here back (see Audit#history). With a name, the lines about them on each of the sheet's descendants come between
 *  the two: each sheet's newest first, the sheets in order of id. */
public class RespondWithAuditLines {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "name", required = false) String name, Audit audit, PersonRepository people,
            SettingRepository settings,
            ObjectResponse<List<String>> response) {
        String sheet = sheetId.trim();
        String filter = name == null ? "" : name;
        List<String> lines = new ArrayList<>(audit.lines(sheet, filter));
        if (!filter.isBlank()) {
            settings.descendants(sheet).stream().sorted().forEach((d) -> lines.addAll(audit.lines(d, filter)));
        }
        Person person = filter.isBlank() ? null : people.findByName(sheet, filter.trim());
        if (person != null && person.origin() != null) {
            lines.addAll(audit.history(person.origin(), person.name(), sheet));
        }
        response.send(lines);
    }
}
