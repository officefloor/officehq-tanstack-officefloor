package net.officefloor.hq.app.audit;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's audit lines as the audit file holds them, newest first; with a name, only those about
 *  that person (name=/person= it, compared as names are), and when that person on this sheet was
 *  transferred here, after them the lines about them from the sheet they came from, newest first. */
public class ListAudit {
    public void list(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "name", required = false) String name, Audit audit,
            PersonRepository people, ObjectResponse<List<String>> response) {
        String sheet = sheetId.trim();
        List<String> lines = new ArrayList<>(audit.lines(sheet, name));
        if (name != null && !name.isBlank()) {
            Person person = people.findByName(sheet, name);
            if (person != null && person.getOrigin() != null && !person.getOrigin().equals(sheet)) {
                lines.addAll(audit.lines(person.getOrigin(), name));
            }
        }
        response.send(lines);
    }
}
