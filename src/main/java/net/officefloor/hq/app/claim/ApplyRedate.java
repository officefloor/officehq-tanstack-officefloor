package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.RequestBody;

public class ApplyRedate {
    // Only a steward re-dates, and only an OPEN or PENDING claim, to a real date not after its person's retirement and
    // not inside a closed period. Its due-by and statement figure stay. While the sheet numbers by period, a claim whose
    // new date falls in another period is numbered next in that period's sequence (the old number is never reused).
    public void apply(@RequestBody(required = false) RedateRequest body, @Val Claim claim, Claims claims, People people,
            Periods periods, Settings settings, Sheets sheets, ServerHttpConnection connection,
            Out<ClaimResponse> responseOut) throws ClaimRedateRefusedException, ClaimAfterRetirementException {
        String user = Audit.user(connection).trim();
        if (!Settings.STEWARD.equals(settings.role(claim.sheetId(), user))) {
            throw new ClaimRedateRefusedException("Only a steward can re-date a claim.");
        }
        if (!claim.status().isOpen()) {
            throw new ClaimRedateRefusedException("Only an OPEN or PENDING claim can be re-dated.");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(body == null || body.date() == null ? "" : body.date().trim());
        } catch (DateTimeParseException e) {
            throw new ClaimRedateRefusedException("Re-date to a date (yyyy-mm-dd).");
        }
        RefuseRetiredInput.check(people.find(claim.sheetId(), claim.personId()), date);
        Period closed = periods.closedOn(claim.sheetId(), date);
        if (closed != null) {
            throw new ClaimRedateRefusedException("The period " + closed.name() + " is closed: no claim can be re-dated into it.");
        }
        String scope = null;
        LocalDate numberingFrom = settings.periodNumberingFrom(claim.sheetId());
        if (numberingFrom != null && !sheets.today(claim.sheetId()).isBefore(numberingFrom)) {
            Period was = periods.on(claim.sheetId(), claim.date());
            Period now = periods.on(claim.sheetId(), date);
            String wasName = was == null ? "" : was.name();
            String nowName = now == null ? "" : now.name();
            if (!Objects.equals(wasName, nowName)) {
                scope = nowName;
            }
        }
        Claim redated = claims.redate(claim, date, scope);
        if (redated == null) {
            throw new ClaimRedateRefusedException("The claim has changed since it was loaded.");
        }
        responseOut.set(ClaimResponse.from(redated));
    }
}
