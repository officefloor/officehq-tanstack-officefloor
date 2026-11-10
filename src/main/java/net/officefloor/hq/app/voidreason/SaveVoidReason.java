package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** A reason already on the sheet's list of void reasons is refused. */
public class SaveVoidReason {
    public void save(@Val VoidReasonInput input, @Val Sheet sheet, VoidReasonRepository repository,
            Out<VoidReason> reasonOut) throws InvalidVoidReasonException {
        if (repository.exists(sheet.id(), input.reason())) {
            throw new InvalidVoidReasonException(input.reason() + " is already a void reason");
        }
        VoidReason reason = new VoidReason(sheet.id(), input.reason());
        repository.save(reason);
        reasonOut.set(reason);
    }
}
