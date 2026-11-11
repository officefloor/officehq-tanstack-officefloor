package net.officefloor.hq.app.audit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/**
 * The audit viewer: the sheet's lines read from the audit file (it is not a second record), newest
 * first. With a name in {@code ?name=} only the lines carrying {@code name=} or {@code person=} that
 * name, compared as names are compared (case and surrounding spaces ignored), followed by the
 * matching lines of the sheet's descendants (each sheet's newest first, the sheets in id order). When that name is a
 * person on the sheet transferred from another sheet, the lines about them from the sheet they came
 * from follow, newest first, as that sheet's file holds them.
 */
public class RespondWithAuditLines {
    public void respond(@Val Sheet sheet, Audit audit, PersonRepository people, SheetRepository sheets,
            HttpServletRequest request,
            ObjectResponse<AuditLinesResponse> response) {
        String name = request.getParameter("name");
        name = name == null ? "" : name;
        List<String> lines = new ArrayList<>(audit.linesFor(sheet.id(), name));
        Collections.reverse(lines);
        if (!name.isBlank()) {
            for (String descendant : sheets.descendants(sheet.id())) {
                List<String> below = new ArrayList<>(audit.linesFor(descendant, name));
                Collections.reverse(below);
                lines.addAll(below);
            }
            Person person = people.findByName(sheet.id(), name);
            if (person != null && person.getOriginSheet() != null) {
                List<String> origin = new ArrayList<>(audit.linesFor(person.getOriginSheet(), person.getName()));
                Collections.reverse(origin);
                lines.addAll(origin);
            }
        }
        response.send(new AuditLinesResponse(lines));
    }
}
