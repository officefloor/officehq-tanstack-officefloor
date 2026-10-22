package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Store the void reason, refusing one already on the sheet's list. */
public class SaveVoidReason {
    public void save(@PathVariable("sheetId") String sheetId, @Val VoidReasonInput input, PeriodRepository periods,
            VoidReasonRepository repository, Out<VoidReason> reason) throws InvalidVoidReasonException {
        String id = sheetId.trim();
        periods.lockSheet(id);
        if (repository.exists(id, input.reason())) {
            throw new InvalidVoidReasonException(input.reason() + " is already a void reason.");
        }
        reason.set(repository.add(id, input));
    }
}
