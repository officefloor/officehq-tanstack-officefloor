package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.RequestBody;

public class ApplyRedate {
    // Only a steward re-dates, and only an OPEN or PENDING claim, to a real date not after its person's retirement. Its
    // due-by and statement figure stay, so it stays in the period its due-by falls in, keeping its number.
    public void apply(@RequestBody(required = false) RedateRequest body, @Val Claim claim, Claims claims, People people,
            Settings settings, ServerHttpConnection connection,
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
        Claim redated = claims.redate(claim, date);
        if (redated == null) {
            throw new ClaimRedateRefusedException("The claim has changed since it was loaded.");
        }
        responseOut.set(ClaimResponse.from(redated));
    }
}
