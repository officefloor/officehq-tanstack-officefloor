package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.Dues;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Takes a statement of the person: records their due-total now (the sum of their counting claims'
 *  due figures, n/a ones skipped; n/a when their grade has no rate on the sheet's today and none of
 *  those claims keeps a settled or bound figure) on the sheet's today, and binds each counting claim
 *  to the figure it shows now; a claim that does not count is unbound. */
public class TakeStatement {
    public void take(@Val Person person, PersonRepository people, ClaimRepository claims, RateRepository rates,
            SheetRepository sheets, SettingRepository settings) {
        String sheetId = person.getSheetId();
        LocalDate today = sheets.today(sheetId);
        String floor = settings.floor(sheetId);
        List<Claim> own = claims.listOf(sheetId, person.getId());
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean kept = false;
        int bound = 0;
        for (Claim c : own) {
            if (!c.isCounting()) {
                c.bind(false, null);
            } else {
                Rate rate = rates.find(sheetId, person.getGrade(), LocalDate.parse(c.getDate()));
                String due = c.due(rate == null ? null : Dues.of(c.getAmount(), rate.pct(), floor));
                kept |= c.getSettledDue() != null || (c.isStated() && c.getStatedDue() != null);
                if (due != null) total = total.add(new BigDecimal(due));
                c.bind(true, due);
                bound++;
            }
            claims.updateBinding(c);
        }
        boolean na = rates.find(sheetId, person.getGrade(), today) == null && !kept;
        person.stated(na ? null : total.toPlainString(), today.toString(), bound);
        people.updateStatements(person);
    }
}
