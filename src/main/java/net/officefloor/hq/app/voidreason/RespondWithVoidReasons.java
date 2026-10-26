package net.officefloor.hq.app.voidreason;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithVoidReasons {
    public void respond(@PathVariable("sheetId") String sheetId, VoidReasons reasons,
            ObjectResponse<List<VoidReasonResponse>> response) {
        response.send(reasons.counts(sheetId.trim()));
    }
}
