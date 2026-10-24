package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.sheet.InvalidTodayException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's claims; as of a date (asOf), as they stood at the end of it (only the comments written and lines added by then). */
public class RespondWithClaims {
    public void respond(@Val Sheet sheet, @RequestParam(name = "asOf", required = false) String asOfParam, AsOf asOf,
            ClaimCommentRepository comments, ClaimLineRepository claimLines,
            ObjectResponse<List<ClaimResponse>> response) throws InvalidTodayException {
        LocalDate date = AsOf.dateOf(asOfParam);
        Map<Long, List<String>> lines = asOf.claimLines(sheet.getId(), date);
        Map<Long, List<String>> lined = claimLines.linesOnSheet(sheet.getId(), date);
        Map<Long, List<String>> commented = comments.linesOnSheet(sheet.getId(), date);
        response.send(asOf.claims(sheet.getId(), date).stream()
                .map(c -> ClaimResponse.from(c, lines.getOrDefault(c.getId(), List.of()),
                        commented.getOrDefault(c.getId(), List.of()),
                        lined.getOrDefault(c.getId(), List.of()))).toList());
    }
}
