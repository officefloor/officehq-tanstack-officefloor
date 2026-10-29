package net.officefloor.hq.app.dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.server.http.ServerHttpConnection;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithDashboard {
    // Counted over the sheet and all its descendants, each by its own today.
    // Only a steward (a delegation included) on the sheet's today sees the counts; as of a date, the role then.
    public void respond(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection, Settings settings,
            Sheets sheets, Claims claims, People people, ObjectResponse<DashboardResponse> response) {
        String sheet = sheetId.trim();
        if (!Settings.STEWARD.equals(settings.role(sheet, Audit.user(connection).trim()))) {
            response.send(DashboardResponse.NONE);
            return;
        }
        int pending = 0, review = 0, overdue = 0;
        List<String> tree = new ArrayList<>(List.of(sheet));
        tree.addAll(settings.descendants(sheet));
        for (String each : tree) {
            LocalDate today = sheets.today(each);
            List<Claim> all = claims.list(each);
            Map<Long, Person> byId = people.list(each).stream().collect(Collectors.toMap(Person::id, Function.identity()));
            pending += (int) all.stream().filter(c -> c.status() == ClaimStatus.PENDING).count();
            review += (int) all.stream().filter(c -> c.status() == ClaimStatus.REVIEW).count();
            overdue += (int) all.stream().filter(c -> {
                Person person = byId.get(c.personId());
                return person != null && Rollup.counts(c, person, today) && settings.overdue(c, today);
            }).count();
        }
        response.send(new DashboardResponse(pending, review, overdue));
    }
}
