package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Only a steward may archive the sheet; anyone else is refused and nothing is written. */
public class ArchiveSheet {
    public void archive(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, RoleRepository roles,
            SheetRepository repository, Out<Sheet> sheet) throws ViewerRefusedException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        if (!Role.STEWARD.equals(roles.roleOf(id, user))) {
            throw new ViewerRefusedException("Only a steward may archive the sheet");
        }
        repository.open(id, user);
        repository.archive(id);
        sheet.set(repository.find(id));
    }
}
