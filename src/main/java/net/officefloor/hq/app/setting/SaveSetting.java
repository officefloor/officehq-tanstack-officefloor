package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveSetting {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidSetting setting, Settings settings, Audit audit,
            ObjectResponse<SettingsResponse> response) {
        settings.setClerkLimit(sheetId, setting.value());
        audit.recordOnCommit("SETTING_CHANGED sheet=" + sheetId + " by=" + setting.user()
                + " key=" + setting.key() + " value=" + (setting.value() == null ? "" : setting.value().toPlainString()));
        response.send(settings.load(sheetId));
    }
}
