package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class SaveProofKinds {
    public void save(@Val ProofKinds kinds, @Val Sheet sheet, SettingRepository repository) {
        if (kinds.given()) {
            repository.saveProofKinds(sheet.id(), kinds.value());
        }
    }
}
