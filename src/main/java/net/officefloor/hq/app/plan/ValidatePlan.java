package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import net.officefloor.hq.app.claim.ClaimFormRules;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePlan {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    // The amount must be one the claim form takes for a claim with no correction or reference; every N days a whole
    // number of at least 1; the dates real dates (a blank start or end is the sheet's today), the end not before the start.
    // The person is filled in once loaded.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PlanRequest body, Sheets sheets,
            Out<PlanRequestChecked> checked) throws PlanRefusedException {
        String sheet = sheetId.trim();
        ClaimInput input;
        try {
            input = ClaimFormRules.parse(new ClaimRequest(body == null ? "" : body.amount(), "", "", "", "", ""));
        } catch (InvalidClaimException e) {
            throw new PlanRefusedException(e.getMessage());
        }
        String every = body.every() == null ? "" : body.every().trim();
        if (!every.matches("\\d{1,6}") || Integer.parseInt(every) < 1) {
            throw new PlanRefusedException("Every N days must be a whole number of at least 1.");
        }
        LocalDate today = sheets.today(sheet);
        LocalDate start = date(body.start(), today);
        LocalDate end = date(body.end(), today);
        if (end.isBefore(start)) {
            throw new PlanRefusedException("The end date must be on or after the start date.");
        }
        checked.set(new PlanRequestChecked(input.amount(), start, Integer.parseInt(every), end));
    }

    private static LocalDate date(String typed, LocalDate today) throws PlanRefusedException {
        String d = typed == null ? "" : typed.trim();
        if (d.isEmpty()) {
            return today;
        }
        try {
            return LocalDate.parse(d, ISO);
        } catch (DateTimeParseException e) {
            throw new PlanRefusedException("Not a date (yyyy-mm-dd): " + d);
        }
    }
}
