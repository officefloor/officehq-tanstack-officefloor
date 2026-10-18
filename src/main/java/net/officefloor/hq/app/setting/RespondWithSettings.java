package net.officefloor.hq.app.setting;

import java.util.LinkedHashMap;
import java.util.Map;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's settings. */
public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId, SettingRepository repository,
            ObjectResponse<SettingsResponse> response) {
        String id = sheetId.trim();
        response.send(new SettingsResponse(repository.findBySheetIdAndKey(id, Setting.CLERK_LIMIT)
                .map(Setting::getValue).orElse(null), repository.floor(id).setScale(2).toPlainString(),
                String.valueOf(repository.terms(id)),
                repository.latePct(id).setScale(2).toPlainString(),
                repository.findBySheetIdAndKey(id, Setting.POLICY_DATE).map(Setting::getValue).orElse(null),
                repository.findBySheetIdAndKey(id, Setting.PERIOD_NUMBERING_FROM).map(Setting::getValue).orElse(null),
                gradeTerms(id, repository)));
    }

    private static Map<String, String> gradeTerms(String id, SettingRepository repository) {
        Map<String, String> terms = new LinkedHashMap<>();
        for (String grade : Setting.GRADES) {
            repository.gradeTerms(id, grade).ifPresent((t) -> terms.put(grade, String.valueOf(t)));
        }
        return terms;
    }
}
