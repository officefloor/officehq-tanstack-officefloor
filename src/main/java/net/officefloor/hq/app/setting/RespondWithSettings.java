package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The settings in force on the sheet's today, and the floor and late percent recorded by from-date. */
public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId, SettingStore store, SheetStore sheets,
            ObjectResponse<SettingsResponse> response) {
        String sheet = sheetId.trim();
        LocalDate today = sheets.today(sheet);
        response.send(new SettingsResponse(store.find(sheet, SettingStore.CLERK_LIMIT),
                store.floors(sheet).apply(today).setScale(2).toPlainString(),
                String.valueOf(store.terms(sheet, today)),
                store.latePcts(sheet).apply(today).setScale(2).toPlainString(),
                store.find(sheet, SettingStore.POLICY_DATE),
                store.find(sheet, SettingStore.PERIOD_NUMBERING_FROM), gradeTerms(sheet, today, store),
                store.find(sheet, SettingStore.CAP), store.find(sheet, SettingStore.PROOF_THRESHOLD),
                history(sheet, SettingStore.FLOOR, store), history(sheet, SettingStore.LATE_PCT, store)));
    }

    private static Map<String, String> gradeTerms(String sheetId, LocalDate today, SettingStore store) {
        Map<String, String> terms = new LinkedHashMap<>();
        for (String grade : SettingStore.GRADES) {
            terms.put(grade, String.valueOf(store.terms(sheetId, grade, today)));
        }
        return terms;
    }

    private static List<SettingsResponse.Dated> history(String sheetId, String key, SettingStore store) {
        List<SettingsResponse.Dated> values = new ArrayList<>();
        store.history(sheetId, key).forEach((from, value) -> values.add(new SettingsResponse.Dated(from.toString(), value)));
        return values;
    }
}
