package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class SetRole {
    public void set(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @Val RoleRequest request, SheetRepository sheets, RoleRepository roles, Out<Role> role) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        sheets.open(id, user);
        role.set(roles.set(id, request.user(), request.role(), user));
    }
}
