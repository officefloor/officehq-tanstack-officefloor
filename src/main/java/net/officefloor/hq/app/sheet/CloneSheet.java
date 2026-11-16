package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Clones the sheet to another id (see CloneRepository). Only a steward may clone, and only to an id never used. */
public class CloneSheet {
    public void cloneSheet(@PathVariable("sheetId") String sheetId, @Val CloneRequest request, CloneRepository clones,
            RoleRepository roles, Out<SheetClone> clone) throws InvalidCloneException {
        String id = sheetId.trim();
        if (!RoleRepository.STEWARD.equals(roles.roleOf(id, request.by()))) {
            throw new InvalidCloneException("Only a steward may clone the sheet");
        }
        if (request.to().isEmpty() || request.to().contains("/") || request.to().equals(id)
                || !clones.unused(request.to())) {
            throw new InvalidCloneException("The sheet can only be cloned to an id never used");
        }
        clones.copy(id, request.to());
        clone.set(new SheetClone(id, request.to()));
    }
}
