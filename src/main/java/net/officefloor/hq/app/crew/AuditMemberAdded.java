package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a crew member added: CREW_MEMBER_ADDED sheet=<id> by=<user> crew=<crew> name=<person>. */
public class AuditMemberAdded {
    public void audit(@PathVariable("sheetId") String sheetId, @Val MemberInput input, Audit audit) {
        audit.record("CREW_MEMBER_ADDED sheet=" + sheetId.trim() + " by=" + input.user() + " crew="
                + input.crew().name() + " name=" + input.personName());
    }
}
