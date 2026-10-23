package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's settings as they are now (as they stood at the end of the as-of date, when one is given;
 *  in force on the preview date, when one is given). */
public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf,
            @RequestParam(name = "preview", required = false) String preview, SettingRepository repository,
            ObjectResponse<SettingsResponse> response) {
        String id = sheetId.trim();
        LocalDate end = AsOf.parse(asOf);
        LocalDate previewed = AsOf.parse(preview);
        response.send(SettingsResponse.from(end != null ? repository.asOf(id, end)
                : previewed != null ? repository.on(id, previewed) : repository.of(id)));
    }
}
