package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class ValidateSheetId {
    public void validate(@PathVariable("sheetId") String sheetId, Out<SheetResponse> sheet)
            throws BadRequestException {
        String id = sheetId == null ? "" : sheetId.trim();
        if (id.isEmpty() || id.length() > 200) {
            throw new BadRequestException("Enter a sheet id");
        }
        sheet.set(new SheetResponse(id));
    }
}
