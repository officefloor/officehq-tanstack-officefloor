package net.officefloor.hq.app.voidreason;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's void reasons in the order added, each with the count of VOID claims voided with it. */
public class RespondWithVoidReasons {
    public void respond(@PathVariable("sheetId") String sheetId, VoidReasonRepository reasons,
            ObjectResponse<List<VoidReason>> response) {
        response.send(reasons.list(sheetId.trim()));
    }
}
