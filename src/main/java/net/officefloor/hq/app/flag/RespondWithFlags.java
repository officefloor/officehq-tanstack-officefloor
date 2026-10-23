package net.officefloor.hq.app.flag;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The flags on the sheet in force on its today. */
public class RespondWithFlags {
    public void respond(@PathVariable("sheetId") String sheetId, FlagRepository flags,
            ObjectResponse<List<FlagResponse>> response) {
        response.send(flags.allInForce(sheetId.trim()));
    }
}
