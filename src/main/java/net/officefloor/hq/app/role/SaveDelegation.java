package net.officefloor.hq.app.role;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveDelegation {
    public void save(@PathVariable("sheetId") String sheetId, @Val Delegation delegation, DelegationStore store) {
        store.add(sheetId.trim(), delegation);
    }
}
