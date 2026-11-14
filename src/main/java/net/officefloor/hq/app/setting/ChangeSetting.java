package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class ChangeSetting {
    public void set(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @Val SettingRequest request, SheetRepository sheets, SettingRepository settings,
            Out<Setting> setting) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        sheets.open(id, user);
        setting.set(settings.set(id, request.key(), request.value(), user));
    }
}
