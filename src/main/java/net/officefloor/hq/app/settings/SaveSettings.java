package net.officefloor.hq.app.settings;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Save the settings given. A setting with a figure is recorded from its from-date (the sheet's today when
 *  none is given), replacing a value recorded from that date and keeping the others; its record ends with
 *  from=<date> when a from-date was given. */
public class SaveSettings {
    public void save(@PathVariable("sheetId") String sheetId, @Val SettingsInput input,
            ServerHttpConnection connection, SettingsRepository repository, SheetRepository sheets, Audit audit,
            Rollup rollup, Out<Settings> settings) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        LocalDate today = sheets.today(id);
        Rollup.Snapshot before = rollup.take(id);
        String prefix = "SETTING_CHANGED sheet=" + id + " by=" + by + " key=";
        if (input.setsClerkLimit()) {
            repository.setClerkLimit(id, from(input, "clerk-limit", today), input.clerkLimit());
            audit.record(prefix + "clerk-limit value="
                    + (input.clerkLimit() == null ? "" : SettingsRepository.money(input.clerkLimit())) + given(input, "clerk-limit"));
        }
        if (input.setsFloor()) {
            repository.setFloor(id, from(input, "floor", today), input.floor());
            audit.record(prefix + "floor value=" + SettingsRepository.money(input.floor()) + given(input, "floor"));
        }
        if (input.setsTerms()) {
            repository.setTerms(id, from(input, "terms", today), input.terms());
            audit.record(prefix + "terms value=" + input.terms() + given(input, "terms"));
        }
        input.gradeTerms().forEach((grade, terms) -> {
            repository.setGradeTerms(id, grade, from(input, "terms-" + grade, today), terms);
            audit.record(prefix + "terms-" + grade + " value=" + (terms == null ? "" : terms) + given(input, "terms-" + grade));
        });
        if (input.setsLatePct()) {
            repository.setLatePct(id, from(input, "late-pct", today), input.latePct());
            audit.record(prefix + "late-pct value=" + SettingsRepository.money(input.latePct()) + given(input, "late-pct"));
        }
        if (input.setsPolicyDate()) {
            repository.setPolicyDate(id, input.policyDate());
            audit.record(prefix + "policy-date value="
                    + (input.policyDate() == null ? "" : input.policyDate().toString()));
        }
        if (input.setsPeriodNumberingFrom()) {
            repository.setPeriodNumberingFrom(id, input.periodNumberingFrom());
            audit.record(prefix + "period-numbering-from value="
                    + (input.periodNumberingFrom() == null ? "" : input.periodNumberingFrom().toString()));
        }
        if (input.setsCap()) {
            repository.setCap(id, from(input, "cap", today), input.cap());
            audit.record(prefix + "cap value="
                    + (input.cap() == null ? "" : SettingsRepository.money(input.cap())) + given(input, "cap"));
        }
        if (input.setsProofThreshold()) {
            repository.setProofThreshold(id, from(input, "proof-threshold", today), input.proofThreshold());
            audit.record(prefix + "proof-threshold value="
                    + (input.proofThreshold() == null ? "" : SettingsRepository.money(input.proofThreshold()))
                    + given(input, "proof-threshold"));
        }
        input.permissions().forEach((action, roles) -> {
            repository.setPermission(id, action, roles);
            audit.record(prefix + "perm-" + action + " value=" + roles);
        });
        rollup.record(id, by, before);
        settings.set(repository.load(id));
    }

    private static LocalDate from(SettingsInput input, String key, LocalDate today) {
        return input.from().getOrDefault(key, today);
    }

    private static String given(SettingsInput input, String key) {
        LocalDate from = input.from().get(key);
        return from == null ? "" : " from=" + from;
    }
}
