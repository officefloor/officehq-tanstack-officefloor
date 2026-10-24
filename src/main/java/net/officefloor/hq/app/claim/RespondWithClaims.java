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

/** The sheet's claims; as of a date (asOf), as they stood at the end of it. */
public class RespondWithClaims {
    public void respond(@Val Sheet sheet, @RequestParam(name = "asOf", required = false) String asOfParam, AsOf asOf,
            ObjectResponse<List<ClaimResponse>> response) throws InvalidTodayException {
        LocalDate date = AsOf.dateOf(asOfParam);
        Map<Long, List<String>> lines = asOf.claimLines(sheet.getId(), date);
        response.send(asOf.claims(sheet.getId(), date).stream()
                .map(c -> ClaimResponse.from(c, lines.getOrDefault(c.getId(), List.of()))).toList());
    }
}
