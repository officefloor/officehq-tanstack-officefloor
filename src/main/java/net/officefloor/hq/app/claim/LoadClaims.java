package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.asof.AsOfView;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's claims; with an as-of date, as they stood at the end of it. */
public class LoadClaims {
    public void service(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, Claims claims, AsOfView view,
            ObjectResponse<List<ClaimResponse>> response) {
        LocalDate date = AsOfView.parse(asOf);
        response.send(date == null ? ClaimResponse.from(claims.list(sheetId)) : view.claims(sheetId, date));
    }
}
