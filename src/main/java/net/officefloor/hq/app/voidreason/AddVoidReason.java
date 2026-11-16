package net.officefloor.hq.app.voidreason;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Stores the void reason on the sheet; one already on its list is refused. */
public class AddVoidReason {
    public void add(@PathVariable("sheetId") String sheetId, @Val VoidReasonValues values, VoidReasonRepository reasons)
            throws InvalidVoidReasonException {
        if (!reasons.add(sheetId.trim(), values.reason())) {
            throw new InvalidVoidReasonException(values.reason() + " is already a void reason");
        }
    }
}
