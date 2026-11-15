package net.officefloor.hq.app.setting;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The first step of every change: a viewer is refused before anything is read or written. */
public class RefuseViewer {
    public void refuse(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, RoleRepository roles)
            throws ViewerRefusedException {
        if (Role.VIEWER.equals(roles.roleOf(sheetId.trim(), by == null ? "" : by))) {
            throw new ViewerRefusedException("A viewer may change nothing");
        }
    }
}
