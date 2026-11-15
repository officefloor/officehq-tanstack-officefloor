package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class Delegate {
    public void delegate(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @Val DelegationRequest request, SheetRepository sheets, DelegationRepository delegations,
            Out<Delegation> delegation) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        sheets.open(id, user);
        delegation.set(delegations.add(id, request.user(), LocalDate.parse(request.from()),
                LocalDate.parse(request.to()), user));
    }
}
