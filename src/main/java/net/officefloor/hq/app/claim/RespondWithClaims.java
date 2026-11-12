package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.web.ObjectResponse;
import net.officefloor.hq.app.history.History;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's claims as they are now, or, as of a date, as they stood at the end of it (see {@link History}),
 *  with their log lines logged and comments written by then and the date standing in for the sheet's today. */
public class RespondWithClaims {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, ClaimStore store, ClaimLogStore log, ClaimCommentStore comments,
            SheetStore sheets, ObjectResponse<List<ClaimResponse>> response) {
        LocalDate on = History.date(asOf);
        List<Claim> claims = on == null ? store.list(sheetId.trim()) : store.listAsOf(sheetId.trim(), on);
        // Only the dates the claims round by are read here, so no policy date is needed.
        Rounding rounding = new Rounding(null, claims);
        LocalDate today = on == null ? sheets.today(sheetId.trim()) : on;
        Map<Long, List<String>> lines = log.lines(sheetId.trim(), on);
        Map<Long, List<String>> commented = comments.lines(sheetId.trim(), on);
        response.send(claims.stream()
                .map(c -> ClaimResponse.from(c, rounding).withLog(lines.getOrDefault(c.getId(), List.of()))
                        .withComments(commented.getOrDefault(c.getId(), List.of())).on(today)).toList());
    }
}
