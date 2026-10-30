package net.officefloor.hq.app.voidreason;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds the reason to the sheet's list. */
public class SaveVoidReason {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidVoidReason reason, VoidReasons reasons, Audit audit,
            ObjectResponse<List<String>> response) {
        reasons.add(sheetId, reason.reason());
        audit.recordOnCommit("VOID_REASON_ADDED sheet=" + sheetId + " by=" + reason.user() + " reason=" + reason.reason());
        response.send(reasons.list(sheetId));
    }
}
