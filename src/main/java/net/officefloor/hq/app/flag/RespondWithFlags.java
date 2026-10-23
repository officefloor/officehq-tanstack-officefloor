package net.officefloor.hq.app.flag;

import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The flags on the sheet in force on its today (or on the preview date, when one is given). */
public class RespondWithFlags {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "preview", required = false) String preview, FlagRepository flags,
            ObjectResponse<List<FlagResponse>> response) {
        response.send(flags.allInForce(sheetId.trim(), AsOf.parse(preview)));
    }
}
