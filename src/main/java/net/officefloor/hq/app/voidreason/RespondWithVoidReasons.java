package net.officefloor.hq.app.voidreason;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's void reasons, in the order they were added. */
public class RespondWithVoidReasons {
    public void respond(@Val Sheet sheet, VoidReasonRepository repository, ObjectResponse<List<VoidReasonResponse>> response) {
        response.send(repository.findBySheetIdOrderByIdAsc(sheet.getId()).stream().map(VoidReasonResponse::from).toList());
    }
}
