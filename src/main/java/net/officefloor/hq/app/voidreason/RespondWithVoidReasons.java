package net.officefloor.hq.app.voidreason;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.ClaimDue;
import net.officefloor.hq.app.claim.ClaimLogLine;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's void reasons in the order added, each with the count of VOID claims voided with it. Read as of a date
 *  (see AsOf), a claim counts only while its log shows it VOID then. */
public class RespondWithVoidReasons {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, VoidReasonRepository reasons,
            ClaimLogRepository log, ObjectResponse<List<VoidReason>> response) {
        String sheet = sheetId.trim();
        LocalDate date = AsOf.parse(asOf);
        List<VoidReason> now = reasons.list(sheet);
        if (date == null) {
            response.send(now);
            return;
        }
        Map<Long, List<ClaimLogLine>> logs = log.bySheet(sheet);
        Map<Long, String> voided = reasons.reasons(sheet);
        List<VoidReason> out = new ArrayList<>();
        for (VoidReason r : now) {
            long count = voided.entrySet().stream().filter((e) -> e.getValue().equals(r.reason())
                    && ClaimDue.statusAsOf(logs.getOrDefault(e.getKey(), List.of()), date) == ClaimStatus.VOID).count();
            out.add(new VoidReason(r.reason(), count));
        }
        response.send(out);
    }
}
