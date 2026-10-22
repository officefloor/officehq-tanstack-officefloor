package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's settings as they are now (as they stood at the end of the as-of date, when one is given). */
public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, SettingRepository repository,
            ObjectResponse<SettingsResponse> response) {
        String id = sheetId.trim();
        LocalDate end = AsOf.parse(asOf);
        response.send(SettingsResponse.from(end != null ? repository.asOf(id, end) : repository.of(id)));
    }
}
