package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class AddVoidReason {
    public void add(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @Val VoidReasonRequest request, SheetRepository sheets, VoidReasonRepository reasons,
            Out<VoidReason> reason) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        sheets.open(id, user);
        reason.set(reasons.add(id, request.reason(), user));
    }
}
