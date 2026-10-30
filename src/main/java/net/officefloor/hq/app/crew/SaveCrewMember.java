package net.officefloor.hq.app.crew;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds the person to the crew. */
public class SaveCrewMember {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidCrewMember member, Crews crews, Audit audit,
            ObjectResponse<List<CrewResponse>> response) {
        crews.addMember(sheetId, member.crew(), member.personId());
        audit.recordOnCommit("CREW_MEMBER_ADDED sheet=" + sheetId + " by=" + member.user() + " crew=" + member.crew()
                + " name=" + member.name());
        response.send(crews.list(sheetId));
    }
}
