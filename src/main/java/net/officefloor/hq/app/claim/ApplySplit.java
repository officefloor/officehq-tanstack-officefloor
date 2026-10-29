package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.holiday.Holidays;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.RequestBody;

public class ApplySplit {

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    // Only an OPEN claim that is not deferred is split, by a role the permission matrix allows to open a claim (the new
    // claim is the user's to open), and only of an amount more than 0.00 and less than the claim's. The claim keeps the
    // rest and loses any statement figure; the new claim is opened now: unnumbered until it is settled, opened by the user, due by its date plus its person's terms in force on the sheet's today (when it is made), moved past the sheet's holidays.
    public void apply(@RequestBody(required = false) SplitRequest body, @Val Claim claim, Claims claims, People people,
            Settings settings, Holidays holidays, Sheets sheets, Periods periods, Rollup rollup, ServerHttpConnection connection,
            Out<ClaimResponse> responseOut, Out<Split> splitOut) throws ClaimSplitRefusedException {
        String user = Audit.user(connection).trim();
        if (!settings.permits(claim.sheetId(), "open", user)) {
            throw new ClaimSplitRefusedException("Your role is not allowed to open a claim, so it cannot split one.");
        }
        LocalDate today = sheets.today(claim.sheetId());
        if (claim.status() != ClaimStatus.OPEN) {
            throw new ClaimSplitRefusedException("A " + claim.status() + " claim cannot be split.");
        }
        if (claim.deferredOn(today)) {
            throw new ClaimSplitRefusedException("The claim is deferred until " + claim.deferredUntil() + ": it cannot be split.");
        }
        String typed = body == null || body.amount() == null ? "" : body.amount().trim();
        if (!AMOUNT.matcher(typed).matches()) {
            throw new ClaimSplitRefusedException("The amount must be a number with up to two decimals.");
        }
        BigDecimal amount = new BigDecimal(typed).setScale(2);
        if (amount.signum() <= 0 || amount.compareTo(claim.amount()) >= 0) {
            throw new ClaimSplitRefusedException("Split an amount more than 0.00 and less than the claim's "
                    + claim.amount().toPlainString() + ".");
        }
        Person person = people.find(claim.sheetId(), claim.personId());
        int terms = person == null ? settings.termsNow(claim.sheetId(), null) : person.termsNow(settings);
        LocalDate dueBy = holidays.dueBy(claim.sheetId(), claim.date(), terms);
        Claim created = claims.split(claim, amount, user, dueBy, terms);
        if (created == null) {
            throw new ClaimSplitRefusedException("The claim has changed since it was loaded.");
        }
        claims.log(created, user, today, rollup.figure(created));
        responseOut.set(ClaimResponse.from(created));
        splitOut.set(new Split(claim.withAmountUnbound(claim.amount().subtract(amount)), created));
    }
}
