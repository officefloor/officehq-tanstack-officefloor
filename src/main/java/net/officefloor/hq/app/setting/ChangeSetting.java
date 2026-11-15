package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class ChangeSetting {
    public void set(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @Val SettingRequest request, SheetRepository sheets, SettingRepository settings,
            RateRepository rates, Out<Setting> setting) throws InvalidSettingException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        if (SettingRepository.PARENT.equals(request.key()) && request.value() != null) {
            String parent = request.value();
            if (parent.equals(id)) {
                throw new InvalidSettingException("A sheet cannot be its own parent");
            }
            if (sheets.find(parent) == null) {
                throw new InvalidSettingException("The parent must be a sheet that exists");
            }
            // Walk up from the parent: meeting this sheet would make a cycle.
            Set<String> seen = new HashSet<>();
            for (String at = rates.parent(parent); at != null && seen.add(at); at = rates.parent(at)) {
                if (at.equals(id)) {
                    throw new InvalidSettingException("The parent cannot be a sheet that has this sheet as an ancestor");
                }
            }
        }
        sheets.open(id, user);
        // A dated key's value applies from its from-date, by default the sheet's today.
        LocalDate from = !SettingRepository.isDated(request.key()) ? null
                : request.from() != null ? LocalDate.parse(request.from()) : sheets.today(id);
        setting.set(settings.set(id, request.key(), request.value(), from, user));
    }
}
