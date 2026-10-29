package net.officefloor.hq.app.approver;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithApprovers {
    public void respond(@PathVariable("sheetId") String sheetId, Approvers approvers,
            ObjectResponse<List<Approver>> response) {
        response.send(approvers.list(sheetId.trim()));
    }
}
