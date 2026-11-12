package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.History;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The ids of the sheet's descendants: the sheets naming it as their parent, their children, and so on; as
 *  they are now, or as each sheet named its parent at the end of the {@code asOf} date. */
public class RespondWithDescendants {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, SettingStore store,
            ObjectResponse<List<String>> response) {
        LocalDate on = History.date(asOf);
        response.send((on == null ? store : store.asOf(on)).descendants(sheetId.trim()));
    }
}
