package net.officefloor.hq.app.voidreason;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's void reasons as the screen reads them, in the order they were added. */
public class RespondWithVoidReasons {
    public void respond(@Val VoidReasons reasons, ObjectResponse<List<String>> response) {
        response.send(reasons.list());
    }
}
