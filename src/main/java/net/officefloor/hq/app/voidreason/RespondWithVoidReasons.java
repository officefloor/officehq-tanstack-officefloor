package net.officefloor.hq.app.voidreason;

import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithVoidReasons {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf,
            @RequestParam(value = "preview", required = false) String preview, VoidReasons reasons, AsOf asOfSheet,
            ObjectResponse<List<VoidReasonResponse>> response) throws InvalidSheetRequestException {
        response.send(asOfSheet.view(sheetId, asOf, preview,
                () -> reasons.onSheet(sheetId).stream().map(VoidReasonResponse::new).toList()));
    }
}
