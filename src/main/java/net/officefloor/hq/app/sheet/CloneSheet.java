package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** A steward clones the sheet to another id, which must never have been used (see {@link SheetCloner}).
 *  Refused (nothing written) for anyone but a steward and for a sheet already used. */
public class CloneSheet {
    public void cloneSheet(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, @Val CloneRequest request,
            RoleRepository roles, SheetRepository sheets, SheetCloner cloner, ClaimRepository claims,
            PeriodRepository periods, SettingRepository settings, Out<Clone> clone)
            throws ViewerRefusedException, InvalidSheetCloneException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        if (!Role.STEWARD.equals(roles.roleOf(id, user))) {
            throw new ViewerRefusedException("Only a steward may clone the sheet");
        }
        if (sheets.find(request.to()) != null) {
            throw new InvalidSheetCloneException("The sheet is already used: " + request.to());
        }
        sheets.open(id, user);
        cloner.cloneSheet(id, request.to(), user, claims, periods, settings, sheets);
        clone.set(new Clone(id, request.to()));
    }
}
