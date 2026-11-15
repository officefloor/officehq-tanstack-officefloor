package net.officefloor.hq.app.voidreason;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListVoidReasons {
    public void list(@PathVariable("sheetId") String sheetId, VoidReasonRepository reasons,
            ObjectResponse<List<VoidReason>> response) {
        response.send(reasons.list(sheetId.trim()));
    }
}
