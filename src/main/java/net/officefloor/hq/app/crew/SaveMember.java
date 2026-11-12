package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveMember {
    public void save(@PathVariable("sheetId") String sheetId, @Val MemberInput input, CrewStore store) {
        store.addMember(sheetId.trim(), input.crew().id(), input.personId());
    }
}
