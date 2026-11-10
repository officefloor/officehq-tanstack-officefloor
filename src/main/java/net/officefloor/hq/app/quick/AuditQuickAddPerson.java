package net.officefloor.hq.app.quick;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.AuditPersonAdded;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the person the quick-add added, as adding them through the person form does; nothing when they were on the sheet. */
public class AuditQuickAddPerson {
    public void audit(@Val QuickAddPerson quick, @Val Sheet sheet, @Val Person person,
            HttpServletRequest request, Audit audit) {
        if (quick.created()) new AuditPersonAdded().audit(sheet, person, request, audit);
    }
}
