package net.officefloor.hq.app.setting;

import java.util.LinkedHashMap;
import java.util.Map;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId, SettingStore store,
            ObjectResponse<SettingsResponse> response) {
        response.send(new SettingsResponse(store.find(sheetId.trim(), SettingStore.CLERK_LIMIT),
                store.floor(sheetId.trim()).setScale(2).toPlainString(),
                String.valueOf(store.terms(sheetId.trim())),
                store.latePct(sheetId.trim()).setScale(2).toPlainString(),
                store.find(sheetId.trim(), SettingStore.POLICY_DATE),
                store.find(sheetId.trim(), SettingStore.PERIOD_NUMBERING_FROM), gradeTerms(sheetId.trim(), store)));
    }

    private static Map<String, String> gradeTerms(String sheetId, SettingStore store) {
        Map<String, String> terms = new LinkedHashMap<>();
        for (String grade : SettingStore.GRADES) {
            terms.put(grade, String.valueOf(store.terms(sheetId, grade)));
        }
        return terms;
    }
}
