package net.officefloor.hq.app.approver;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The approvers on the sheet, by person id. */
public class RespondWithApprovers {
    public void respond(@PathVariable("sheetId") String sheetId, ApproverRepository approvers,
            ObjectResponse<List<ApproverResponse>> response) {
        response.send(approvers.all(sheetId.trim()));
    }
}
