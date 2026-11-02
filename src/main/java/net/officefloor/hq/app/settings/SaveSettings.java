package net.officefloor.hq.app.settings;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveSettings {
    public void save(@PathVariable("sheetId") String sheetId, @Val SettingsInput input,
            ServerHttpConnection connection, SettingsRepository repository, Audit audit, Rollup rollup,
            Out<Settings> settings) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        Rollup.Snapshot before = rollup.take(id);
        if (input.setsClerkLimit()) {
            repository.setClerkLimit(id, input.clerkLimit());
            audit.record("SETTING_CHANGED sheet=" + id + " by=" + by + " key=clerk-limit value="
                    + (input.clerkLimit() == null ? "" : SettingsRepository.money(input.clerkLimit())));
        }
        if (input.setsFloor()) {
            repository.setFloor(id, input.floor());
            audit.record("SETTING_CHANGED sheet=" + id + " by=" + by + " key=floor value="
                    + SettingsRepository.money(input.floor()));
        }
        if (input.setsTerms()) {
            repository.setTerms(id, input.terms());
            audit.record("SETTING_CHANGED sheet=" + id + " by=" + by + " key=terms value=" + input.terms());
        }
        if (input.setsLatePct()) {
            repository.setLatePct(id, input.latePct());
            audit.record("SETTING_CHANGED sheet=" + id + " by=" + by + " key=late-pct value="
                    + SettingsRepository.money(input.latePct()));
        }
        if (input.setsPolicyDate()) {
            repository.setPolicyDate(id, input.policyDate());
            audit.record("SETTING_CHANGED sheet=" + id + " by=" + by + " key=policy-date value="
                    + (input.policyDate() == null ? "" : input.policyDate().toString()));
        }
        rollup.record(id, by, before);
        settings.set(repository.load(id));
    }
}
