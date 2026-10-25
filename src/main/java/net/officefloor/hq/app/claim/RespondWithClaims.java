package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.SheetHistory;
import net.officefloor.hq.app.history.ViewAsOf;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaims {
    public void respond(@Val SheetResponse sheet, @Val ViewAsOf asOf, SheetHistory history,
            ObjectResponse<List<ClaimResponse>> response) {
        response.send(history.view(sheet.id(), asOf.on(), jdbc -> {
            Map<Long, List<String>> lines = new ClaimLogRepository(jdbc).lines(sheet.id());
            Map<Long, List<String>> comments = new ClaimCommentRepository(jdbc).lines(sheet.id());
            LocalDate today = new SheetRepository(jdbc).today(sheet.id());
            return new ClaimRepository(jdbc).list(sheet.id()).stream()
                    .map(c -> ClaimResponse.from(c, lines.getOrDefault(c.getId(), List.of()),
                            comments.getOrDefault(c.getId(), List.of()), today)).toList();
        }));
    }
}
