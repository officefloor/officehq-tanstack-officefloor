package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.SheetArchivedException;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The first step of every change: a viewer, and any change to an archived sheet, is refused before
 *  anything is read or written. */
public class RefuseViewer {
    public void refuse(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, RoleRepository roles,
            SheetRepository sheets) throws ViewerRefusedException, SheetArchivedException {
        if (sheets.isArchived(sheetId.trim())) {
            throw new SheetArchivedException("The sheet is archived");
        }
        if (Role.VIEWER.equals(roles.roleOf(sheetId.trim(), by == null ? "" : by))) {
            throw new ViewerRefusedException("A viewer may change nothing");
        }
    }
}
