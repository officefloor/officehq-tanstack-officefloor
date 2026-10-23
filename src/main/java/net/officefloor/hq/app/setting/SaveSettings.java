package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Save the settings the input sets: each dated one recorded from its from-date (not given: the sheet's today),
 *  keeping the values already recorded (one from that same date is replaced); the policy date and the period
 *  numbering from and the grace days and the pot and the parent sheet and the fiscal year start month and the permission matrix as
 *  they are. */
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
        input.gradeGrace().forEach((grade, days) -> repository.setDated(id, "grace-" + grade,
                days != null ? String.valueOf(days) : null, from(input, "grace-" + grade, today)));
        input.gradeProofThreshold().forEach((grade, amount) -> repository.setDated(id, "proof-threshold-" + grade,
                text(amount), from(input, "proof-threshold-" + grade, today)));
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
        if (input.setsProofThreshold()) {
            repository.setDated(id, "proof-threshold", text(input.proofThreshold()), from(input, "proof-threshold", today));
        }
        if (input.setsNoteThreshold()) {
            repository.setDated(id, "note-threshold", text(input.noteThreshold()), from(input, "note-threshold", today));
        }
        if (input.setsGraceDays()) {
            repository.setGraceDays(id, input.graceDays());
        }
        if (input.setsEntryDate()) {
            repository.setEntryDate(id, input.entryDate());
        }
        if (input.setsPot()) {
            repository.setPot(id, input.pot());
        }
        if (input.setsParent()) {
            repository.setParent(id, input.parent());
        }
        if (input.fiscalStartMonth() != null) {
            repository.setFiscalStartMonth(id, input.fiscalStartMonth());
        }
        input.perms().forEach((action, roles) -> repository.setPermission(id, action, roles));
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
