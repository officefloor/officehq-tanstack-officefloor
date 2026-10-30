package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.asof.AsOfView;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The ids of the people the user watches on the sheet; with an as-of date, those watched at the end of it. */
public class LoadWatches {
    public void service(@PathVariable("sheetId") String sheetId, @PathVariable("user") String user,
            @RequestParam(value = "asOf", required = false) String asOf, Watches watches, AsOfView view,
            ObjectResponse<List<String>> response) {
        LocalDate date = AsOfView.parse(asOf);
        response.send(date == null ? watches.watchedBy(sheetId, user) : view.watches(sheetId, user, date));
    }
}
