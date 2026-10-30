package net.officefloor.hq.app.voidreason;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's void reasons, in the order added. */
public class LoadVoidReasons {
    public void service(@PathVariable("sheetId") String sheetId, VoidReasons reasons, ObjectResponse<List<String>> response) {
        response.send(reasons.list(sheetId));
    }
}
