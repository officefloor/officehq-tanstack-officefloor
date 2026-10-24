package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the void reason to the sheet; refuses one already on its list. */
public class ApplyVoidReason {
    public void apply(@Val VoidReasonRequest request, @Val Sheet sheet, VoidReasonRepository repository,
            Out<VoidReason> reasonOut) throws InvalidVoidReasonException {
        if (repository.existsBySheetIdAndReason(sheet.getId(), request.reason())) {
            throw new InvalidVoidReasonException("That reason is already on the list.");
        }
        reasonOut.set(repository.saveAndFlush(new VoidReason(sheet.getId(), request.reason())));
    }
}
