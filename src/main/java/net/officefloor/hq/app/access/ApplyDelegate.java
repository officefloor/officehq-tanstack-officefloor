package net.officefloor.hq.app.access;

import java.time.LocalDate;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Records a delegation of the steward role: only a steward in their own right may delegate (not one acting by a
 * delegation), and the to-date may not be before the from-date (blank: the sheet's today).
 */
public class ApplyDelegate {
    public void apply(@Val SheetResponse sheet, @Val DelegateInput input, @Val ChangedBy by, RoleRepository roles,
            SheetRepository sheets, DelegationRepository repository, Out<Delegation> delegation)
            throws BadRequestException {
        if (!Role.STEWARD.equals(roles.assignedRoleOf(sheet.id(), by.name()))) {
            throw new BadRequestException("Only a steward delegates the steward role");
        }
        LocalDate from = input.from() == null ? sheets.today(sheet.id()) : input.from();
        if (input.to().isBefore(from)) {
            throw new BadRequestException("A to-date may not be before the from-date");
        }
        delegation.set(repository.insert(sheet.id(), input.user(), from, input.to(), by.name()));
    }
}
