package net.officefloor.hq.app.role;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.SheetArchivedException;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Refuses any change by a viewer, and any change at all on an archived sheet, before anything is
 * read or written. A viewer sees every screen and figure and can change nothing; an archived sheet
 * stays visible to everyone and no one can change it. Runs first on every change endpoint except the
 * bulk ones, which go through with every selected item refused.
 */
public class CheckCanChange {
    public void check(@PathVariable("sheetId") String sheetId, RoleRepository roles, SheetRepository sheets,
            HttpServletRequest request) throws ViewerException, SheetArchivedException {
        String id = sheetId == null ? "" : sheetId.trim();
        String user = Audit.user(request);
        if (Role.VIEWER.equals(roles.roleOf(id, user))) {
            throw new ViewerException("A viewer may not change the sheet");
        }
        if (sheets.isArchived(id)) throw new SheetArchivedException(id);
    }
}
