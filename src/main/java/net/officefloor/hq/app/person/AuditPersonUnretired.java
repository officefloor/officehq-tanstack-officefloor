package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the person just unretired and the day they were unretired (their latest past retirement's to-date). */
public class AuditPersonUnretired {
    public void audit(@Val Person person, @RequestParam("user") String user, Audit audit) {
        audit.record("PERSON_UNRETIRED sheet=" + person.sheetId() + " by=" + user + " name=" + person.name()
                + " on=" + person.pastRetirements().get(person.pastRetirements().size() - 1).to());
    }
}
