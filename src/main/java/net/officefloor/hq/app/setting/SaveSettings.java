package net.officefloor.hq.app.setting;

import java.util.HashMap;
import java.util.Map;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Saves the settings being changed, leaving the others as they are. */
public class SaveSettings {
    public void save(@Val SettingsInput input, @Val Sheet sheet, SettingsRepository repository,
            Out<Settings> settingsOut) {
        Settings now = repository.find(sheet.id());
        Settings settings = new Settings(sheet.id(),
                input.clerkLimitChanged() ? input.clerkLimit() : now.clerkLimit(),
                input.floor() != null ? input.floor() : now.floor(),
                input.terms() != null ? input.terms() : now.terms(),
                input.latePct() != null ? input.latePct() : now.latePct(),
                input.policyDateChanged() ? input.policyDate() : now.policyDate(),
                input.periodNumberingFromChanged() ? input.periodNumberingFrom() : now.periodNumberingFrom(),
                gradeTerms(now, input),
                input.capChanged() ? input.cap() : now.cap());
        repository.save(settings);
        settingsOut.set(settings);
    }

    private static Map<String, Integer> gradeTerms(Settings now, SettingsInput input) {
        Map<String, Integer> terms = new HashMap<>(now.gradeTerms());
        input.gradeTerms().forEach((grade, days) -> {
            if (days == null) terms.remove(grade);
            else terms.put(grade, days);
        });
        return terms;
    }
}
