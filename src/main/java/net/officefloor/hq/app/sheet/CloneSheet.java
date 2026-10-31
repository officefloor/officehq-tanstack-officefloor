package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** A steward clones the sheet to another id (see {@link Clones#copy}). Refused: anyone but a steward,
 *  and another sheet that has been used (this sheet's own id among them). */
public class CloneSheet {
    public void cloneSheet(@PathVariable("sheetId") String sheetId, @Val CloneRequest request, Settings settings,
            Sheets sheets, Clones clones, Out<Clone> cloneOut) throws InvalidSheetRequestException {
        String to = request.to().trim();
        if (settings.role(sheetId, request.user()) != Role.STEWARD) {
            throw new InvalidSheetRequestException("Only a steward can clone the sheet");
        }
        if (to.equals(sheetId) || sheets.exists(to)) {
            throw new InvalidSheetRequestException("The other sheet has already been used");
        }
        clones.copy(sheetId, to);
        cloneOut.set(new Clone(sheetId, to, request.user()));
    }
}
