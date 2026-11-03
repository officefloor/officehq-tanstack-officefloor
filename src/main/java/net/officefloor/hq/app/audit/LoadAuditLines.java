package net.officefloor.hq.app.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's audit lines, newest first. Filtered by the name of a person transferred here, the lines about
 *  them from the sheet they came from follow (newest first, exactly as the file holds them). */
public class LoadAuditLines {
    public void load(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "name", required = false) String name, ServerHttpConnection connection,
            Audit audit, PersonRepository people, Out<AuditLines> lines) throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        List<String> found = new ArrayList<>(audit.read(id, name));
        String who = name == null ? "" : name.trim();
        if (!who.isEmpty()) {
            Person person = people.findByName(id, who);
            if (person != null && person.origin() != null
                    && person.name().trim().toLowerCase(Locale.ROOT).equals(who.toLowerCase(Locale.ROOT))) {
                found.addAll(audit.read(person.origin(), who));
            }
        }
        lines.set(new AuditLines(found));
    }
}
