package net.officefloor.hq.app.setting;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Refuses (400) any change by a viewer on the sheet: it runs first, so a refused change writes nothing. */
public class CheckNotViewer {
    public void check(@PathVariable("sheetId") String sheetId, @RequestParam("user") String user,
            SettingRepository settings) throws ViewerRefusedException {
        if (settings.find(sheetId).roleOf(user) == Role.VIEWER) {
            throw new ViewerRefusedException("A viewer cannot change the sheet.");
        }
    }
}
