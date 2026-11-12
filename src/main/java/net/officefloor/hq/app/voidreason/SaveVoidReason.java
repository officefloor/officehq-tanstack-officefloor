package net.officefloor.hq.app.voidreason;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveVoidReason {
    public void save(@PathVariable("sheetId") String sheetId, @Val VoidReasonInput input, VoidReasonStore store) {
        store.add(sheetId.trim(), input.reason());
    }
}
