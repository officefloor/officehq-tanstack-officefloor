package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveMember {
    public void save(@PathVariable("sheetId") String sheetId, @Val MemberInput input, PoolStore store) {
        store.addMember(sheetId.trim(), input.pool().id(), input.personId(), input.share());
    }
}
