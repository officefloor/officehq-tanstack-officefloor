package net.officefloor.hq.app.setting;

import java.time.LocalDate;
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
        // A dated key's value applies from its from-date, by default the sheet's today.
        LocalDate from = !SettingRepository.isDated(request.key()) ? null
                : request.from() != null ? LocalDate.parse(request.from()) : sheets.today(id);
        setting.set(settings.set(id, request.key(), request.value(), from, user));
    }
}
