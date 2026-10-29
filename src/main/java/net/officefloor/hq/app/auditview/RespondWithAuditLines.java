package net.officefloor.hq.app.auditview;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithAuditLines {
    // With a name, this sheet's lines are followed by the matching lines of each of its descendants (by id, each newest
    // first), and then, when the name is (a name or alias of) a person transferred in, by those about them on the sheet they came from (under the name they had there), newest first, as that sheet's are.
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "name", required = false) String name, Audit audit, People people, Settings settings,
            ObjectResponse<List<String>> response) {
        String sheet = sheetId.trim();
        List<String> lines = new ArrayList<>(audit.lines(sheet, name));
        if (name != null && !name.isBlank()) {
            for (String descendant : settings.descendants(sheet)) {
                lines.addAll(audit.lines(descendant, name));
            }
            Person person = people.findByName(sheet, name);
            if (person != null && person.origin() != null) {
                String originName = people.originName(person.id());
                lines.addAll(audit.lines(person.origin(), originName == null ? person.name() : originName));
            }
        }
        response.send(lines);
    }
}
