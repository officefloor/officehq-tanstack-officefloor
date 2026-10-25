package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the void reason; one already on the sheet's list is refused. */
public class BuildVoidReason {
    public void build(@Val SheetResponse sheet, @Val VoidReasonInput input, VoidReasonRepository repository,
            Out<VoidReason> reason) throws BadRequestException {
        if (repository.taken(sheet.id(), input.reason())) {
            throw new BadRequestException(VoidReasonRepository.TAKEN);
        }
        repository.insert(sheet.id(), input.reason());
        reason.set(new VoidReason(sheet.id(), input.reason()));
    }
}
