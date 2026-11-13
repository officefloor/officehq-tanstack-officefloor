package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the person just edited; runs only once the edit has passed every rule. */
public class AuditPersonEdited {
    public void audit(@Val Person person, @Val PersonTermsChange termsChange, @RequestParam("user") String user,
            Audit audit) {
        audit.record("PERSON_EDITED sheet=" + person.sheetId() + " by=" + user + " name=" + person.name()
                + " grade=" + person.grade()
                + (termsChange.changed() ? " terms=" + (person.terms() == null ? "" : person.terms()) : ""));
    }
}
