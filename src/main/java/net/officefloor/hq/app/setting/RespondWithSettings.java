package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.History;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The settings in force on the sheet's today, and the floor and late percent recorded by from-date; as of a
 *  date, as they stood at the end of it, in force on that date. The parent sheet is the one named now. */
public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, SettingStore settings, SheetStore sheets,
            ObjectResponse<SettingsResponse> response) {
        String sheet = sheetId.trim();
        LocalDate on = History.date(asOf);
        SettingStore store = on == null ? settings : settings.asOf(on);
        LocalDate today = on == null ? sheets.today(sheet) : on;
        response.send(new SettingsResponse(store.find(sheet, SettingStore.CLERK_LIMIT),
                store.floors(sheet).apply(today).setScale(2).toPlainString(),
                String.valueOf(store.terms(sheet, today)),
                store.latePcts(sheet).apply(today).setScale(2).toPlainString(),
                store.find(sheet, SettingStore.POLICY_DATE),
                store.find(sheet, SettingStore.PERIOD_NUMBERING_FROM), gradeTerms(sheet, today, store),
                store.find(sheet, SettingStore.CAP), store.find(sheet, SettingStore.PROOF_THRESHOLD),
                history(sheet, SettingStore.FLOOR, store), history(sheet, SettingStore.LATE_PCT, store),
                store.perms(sheet), store.find(sheet, SettingStore.GRACE_DAYS),
                store.find(sheet, SettingStore.NOTE_THRESHOLD), store.find(sheet, SettingStore.ENTRY_DATE),
                gradeGrace(sheet, today, store), gradeGraceFrom(sheet, store), gradeProofThreshold(sheet, today, store),
                store.find(sheet, SettingStore.POT), settings.parent(sheet),
                String.valueOf(store.fiscalStartMonth(sheet)), store.find(sheet, SettingStore.ALERT_THRESHOLD),
                store.find(sheet, SettingStore.ROUNDING), history(sheet, SettingStore.ROUNDING, store),
                store.find(sheet, SettingStore.REVIEW_THRESHOLD), store.find(sheet, SettingStore.ESCALATE_AFTER),
                store.find(sheet, SettingStore.REVIEW_SLA), store.find(sheet, SettingStore.PROOF_KINDS),
                store.find(sheet, SettingStore.DEFAULT_GRADE), gradeClerkLimit(sheet, today, store),
                store.find(sheet, SettingStore.SCORE_MID), store.find(sheet, SettingStore.SCORE_HIGH),
                store.find(sheet, SettingStore.SCORE_LOG), store.find(sheet, SettingStore.MAX_OPEN),
                store.find(sheet, SettingStore.PENDING_SLA)));
    }

    private static Map<String, String> gradeTerms(String sheetId, LocalDate today, SettingStore store) {
        Map<String, String> terms = new LinkedHashMap<>();
        for (String grade : SettingStore.GRADES) {
            terms.put(grade, String.valueOf(store.terms(sheetId, grade, today)));
        }
        return terms;
    }

    private static Map<String, String> gradeGrace(String sheetId, LocalDate today, SettingStore store) {
        Map<String, String> grace = new LinkedHashMap<>();
        for (String grade : SettingStore.GRADES) {
            grace.put(grade, store.findOn(sheetId, SettingStore.GRACE_PREFIX + grade, today));
        }
        return grace;
    }

    private static Map<String, String> gradeProofThreshold(String sheetId, LocalDate today, SettingStore store) {
        Map<String, String> thresholds = new LinkedHashMap<>();
        for (String grade : SettingStore.GRADES) {
            thresholds.put(grade, store.findOn(sheetId, SettingStore.PROOF_THRESHOLD_PREFIX + grade, today));
        }
        return thresholds;
    }

    private static Map<String, String> gradeClerkLimit(String sheetId, LocalDate today, SettingStore store) {
        Map<String, String> limits = new LinkedHashMap<>();
        for (String grade : SettingStore.GRADES) {
            limits.put(grade, store.findOn(sheetId, SettingStore.CLERK_LIMIT_PREFIX + grade, today));
        }
        return limits;
    }

    private static Map<String, List<SettingsResponse.Dated>> gradeGraceFrom(String sheetId, SettingStore store) {
        Map<String, List<SettingsResponse.Dated>> graces = new LinkedHashMap<>();
        for (String grade : SettingStore.GRADES) {
            graces.put(grade, history(sheetId, SettingStore.GRACE_PREFIX + grade, store));
        }
        return graces;
    }

    private static List<SettingsResponse.Dated> history(String sheetId, String key, SettingStore store) {
        List<SettingsResponse.Dated> values = new ArrayList<>();
        store.history(sheetId, key).forEach((from, value) -> values.add(new SettingsResponse.Dated(from.toString(), value)));
        return values;
    }
}
