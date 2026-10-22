package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Save the settings the input sets: each dated one recorded from its from-date (not given: the sheet's today),
 *  keeping the values already recorded (one from that same date is replaced); the policy date and the period
 *  numbering from as they are. */
public class SaveSettings {
    public void save(@PathVariable("sheetId") String sheetId, @Val SettingsInput input, SettingRepository repository,
            Out<Settings> settings) {
        String id = sheetId.trim();
        LocalDate today = repository.today(id);
        if (input.setsClerkLimit()) {
            repository.setDated(id, "clerk-limit", text(input.clerkLimit()), from(input, "clerk-limit", today));
        }
        if (input.floor() != null) {
            repository.setDated(id, "floor", text(input.floor()), from(input, "floor", today));
        }
        if (input.terms() != null) {
            repository.setDated(id, "terms", String.valueOf(input.terms()), from(input, "terms", today));
        }
        input.gradeTerms().forEach((grade, days) -> repository.setDated(id, "terms-" + grade,
                days != null ? String.valueOf(days) : null, from(input, "terms-" + grade, today)));
        if (input.latePct() != null) {
            repository.setDated(id, "late-pct", text(input.latePct()), from(input, "late-pct", today));
        }
        if (input.setsPolicyDate()) {
            repository.setPolicyDate(id, input.policyDate());
        }
        if (input.setsPeriodNumberingFrom()) {
            repository.setPeriodNumberingFrom(id, input.periodNumberingFrom());
        }
        if (input.setsCap()) {
            repository.setDated(id, "cap", text(input.cap()), from(input, "cap", today));
        }
        settings.set(repository.of(id));
    }

    private static LocalDate from(SettingsInput input, String key, LocalDate today) {
        LocalDate from = input.from().get(key);
        return from != null ? from : today != null ? today : LocalDate.of(1, 1, 1);
    }

    private static String text(BigDecimal value) {
        return value != null ? value.toPlainString() : null;
    }
}
