package net.officefloor.hq.app.setting;

import java.util.LinkedHashMap;
import java.util.Map;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId, Settings settings,
            ObjectResponse<SettingsResponse> response) {
        String sheet = sheetId.trim();
        response.send(new SettingsResponse(settings.get(sheet, Settings.CLERK_LIMIT),
                settings.floor(sheet).setScale(2).toPlainString(), Integer.toString(settings.terms(sheet)),
                settings.latePct(sheet).setScale(2).toPlainString(), settings.get(sheet, Settings.POLICY_DATE),
                settings.get(sheet, Settings.PERIOD_NUMBERING_FROM), gradeTerms(settings, sheet),
                settings.get(sheet, Settings.CAP)));
    }

    private static Map<String, String> gradeTerms(Settings settings, String sheet) {
        Map<String, String> terms = new LinkedHashMap<>();
        for (String grade : Settings.GRADES) {
            terms.put(grade, Integer.toString(settings.terms(sheet, grade)));
        }
        return terms;
    }
}
