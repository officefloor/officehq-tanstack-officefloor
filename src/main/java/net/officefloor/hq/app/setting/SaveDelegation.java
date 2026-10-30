package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveDelegation {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidDelegation delegation, Settings settings,
            Audit audit, ObjectResponse<SettingsResponse> response) {
        settings.delegate(sheetId, delegation.name(), delegation.from(), delegation.to(), delegation.user());
        audit.recordOnCommit("DELEGATED sheet=" + sheetId + " by=" + delegation.user() + " user=" + delegation.name()
                + " from=" + delegation.from() + " to=" + delegation.to());
        response.send(settings.load(sheetId));
    }
}
