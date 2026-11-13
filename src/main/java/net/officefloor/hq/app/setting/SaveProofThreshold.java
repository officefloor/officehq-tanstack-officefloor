package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveProofThreshold {
    public void save(@Val ProofThreshold threshold, @Val Sheet sheet, SettingRepository repository) {
        if (threshold.given()) {
            repository.saveDated(sheet.id(), DatedSettings.PROOF_THRESHOLD,
                    DatedSettings.fromOr(threshold.from(), sheet.today()),
                    threshold.value() == null ? null : threshold.value().toPlainString());
        }
    }
}
