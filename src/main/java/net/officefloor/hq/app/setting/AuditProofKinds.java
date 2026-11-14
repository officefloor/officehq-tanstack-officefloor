package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the proof kinds just saved, comma separated (blank: none), even when they are the ones the sheet had; nothing when left as they are. */
public class AuditProofKinds {
    public void audit(@Val ProofKinds kinds, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        if (!kinds.given()) {
            return;
        }
        audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user + " key=proof-kinds value="
                + String.join(",", kinds.value()));
    }
}
