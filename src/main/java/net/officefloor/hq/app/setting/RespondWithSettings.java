package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithSettings {
    public void respond(@PathVariable("sheetId") String sheetId, Settings settings, Sheets sheets,
            ObjectResponse<SettingsResponse> response) {
        String sheet = sheetId.trim();
        LocalDate today = sheets.today(sheet);
        response.send(new SettingsResponse(settings.current(sheet, Settings.CLERK_LIMIT),
                settings.floor(sheet, today).setScale(2).toPlainString(), Integer.toString(settings.terms(sheet, today)),
                settings.latePct(sheet, today).setScale(2).toPlainString(), settings.get(sheet, Settings.POLICY_DATE),
                settings.get(sheet, Settings.PERIOD_NUMBERING_FROM), gradeTerms(settings, sheet, today),
                settings.current(sheet, Settings.CAP), dated(settings, sheet), perms(settings, sheet),
                settings.get(sheet, Settings.GRACE_DAYS), settings.get(sheet, Settings.ENTRY_DATE),
                settings.get(sheet, Settings.POT), settings.parent(sheet),
                Integer.toString(settings.fiscalStartMonth(sheet)), settings.get(sheet, Settings.ALERT_THRESHOLD),
                settings.get(sheet, Settings.ESCALATE_AFTER), settings.get(sheet, Settings.REVIEW_SLA),
                settings.get(sheet, Settings.PROOF_KINDS), settings.get(sheet, Settings.DEFAULT_GRADE),
                settings.get(sheet, Settings.SCORE_MID), settings.get(sheet, Settings.SCORE_HIGH),
                settings.get(sheet, Settings.SCORE_LOG), settings.get(sheet, Settings.MAX_OPEN)));
    }

    private static Map<String, String> gradeTerms(Settings settings, String sheet, LocalDate today) {
        Map<String, String> terms = new LinkedHashMap<>();
        for (String grade : Settings.GRADES) {
            terms.put(grade, Integer.toString(settings.terms(sheet, grade, today)));
        }
        return terms;
    }

    private static Map<String, String> perms(Settings settings, String sheet) {
        Map<String, String> perms = new LinkedHashMap<>();
        for (String action : Settings.PERM_ACTIONS) {
            perms.put(action, settings.perm(sheet, action));
        }
        return perms;
    }

    private static Map<String, List<Settings.DatedValue>> dated(Settings settings, String sheet) {
        Map<String, List<Settings.DatedValue>> dated = new LinkedHashMap<>();
        for (String key : Settings.DATED) {
            dated.put(key, settings.history(sheet, key));
        }
        return dated;
    }
}
