package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import jakarta.servlet.http.HttpServletRequest;

/** Records the alias given. Runs after it is saved. */
public class AuditAliasAdded {
    public void audit(@Val Sheet sheet, @Val Person person, @Val AliasInput input,
            HttpServletRequest request, Audit audit) {
        audit.record("PERSON_ALIAS_ADDED sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " name=" + person.getName() + " alias=" + input.alias());
    }
}
