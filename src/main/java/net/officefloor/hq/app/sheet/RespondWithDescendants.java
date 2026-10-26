package net.officefloor.hq.app.sheet;

import java.util.List;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The ids of the sheet's descendants: the sheets naming it as their parent now, theirs, and so on down. */
public class RespondWithDescendants {
    public void respond(@Val SheetResponse sheet, RateRepository rates, ObjectResponse<List<String>> response) {
        response.send(rates.descendantsOf(sheet.id()));
    }
}
