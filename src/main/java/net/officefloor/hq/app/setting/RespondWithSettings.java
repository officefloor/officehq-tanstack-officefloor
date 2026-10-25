package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.sheet.InvalidTodayException;
import net.officefloor.hq.app.sheet.SettingLogRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's settings; as of a date (asOf), as they stood at the end of it — with the policy versions made by
 *  then. */
public class RespondWithSettings {
    public void respond(@Val Sheet sheet, @RequestParam(name = "asOf", required = false) String asOfParam, AsOf asOf,
            SettingLogRepository settingLogs, ObjectResponse<SettingsResponse> response) throws InvalidTodayException {
        LocalDate date = AsOf.dateOf(asOfParam);
        List<SettingsResponse.PolicyVersion> versions = settingLogs.findBySheetIdOrderByIdAsc(sheet.getId()).stream()
                .filter(set -> Sheet.POLICY_KEYS.contains(set.getKey()))
                .filter(set -> date == null || !set.getMadeOn().isAfter(date))
                .map(set -> new SettingsResponse.PolicyVersion(
                        set.getFromDate() == null ? null : set.getFromDate().toString(), set.getKey(), set.getValue()))
                .toList();
        response.send(SettingsResponse.from(asOf.sheet(sheet, date), versions));
    }
}
