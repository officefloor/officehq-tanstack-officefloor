package net.officefloor.hq.app.voidreason;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with the sheet's void reasons, in the order added, each with its count of VOID claims. */
public class RespondWithVoidReasons {
    public void respond(@PathVariable("sheet") String sheet, VoidReasons reasons, ObjectResponse<List<VoidReason>> response) {
        response.send(reasons.list(sheet));
    }
}
