package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveRole {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidRole role, Settings settings, Audit audit,
            ObjectResponse<SettingsResponse> response) {
        settings.setRole(sheetId, role.name(), role.role());
        audit.recordOnCommit("ROLE_SET sheet=" + sheetId + " by=" + role.user()
                + " user=" + role.name() + " role=" + role.role());
        response.send(settings.load(sheetId));
    }
}
