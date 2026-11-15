package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.Dues;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.DatedSetting;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Takes a statement of the person: records their due-total now (the sum of the due of their claims that
 *  count live (open ones dated within a hold of theirs, and not bound, do not), n/a ones skipped, at most the sheet's cap; n/a when their grade has no rate on the sheet's today and none of
 *  those claims keeps a settled or bound figure, or a live one at a grade no longer in force or dated on or before the sheet's today) on the sheet's today, and binds each counting claim
 *  to its rate figure under the due it shows now (its late charge and the floor are never bound); a claim that does not
 *  count live is unbound. */
public class TakeStatement {
    public void take(@Val Person person, PersonRepository people, ClaimRepository claims, RateRepository rates,
            SheetRepository sheets, SettingRepository settings) {
        String sheetId = person.getSheetId();
        LocalDate today = sheets.today(sheetId);
        DatedSetting floor = settings.floor(sheetId);
        Dues.Late late = settings.late(sheetId, today);
        List<Claim> own = claims.listOf(sheetId, person.getId());
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean kept = false;
        int bound = 0;
        for (Claim c : own) {
            if (!person.countsLive(c)) {
                c.bind(false, null);
            } else {
                String grade = person.gradeOn(c.getDate());
                Rate rate = rates.find(sheetId, grade, LocalDate.parse(c.getDate()));
                String live = rate == null ? null : Dues.of(c.getAmount(), rate.pct(), late.rounding(c.getRoundDate()));
                String shown = Dues.shown(c, live, floor.on(c.getDate()), late);
                kept |= c.getSettledDue() != null || c.isBound()
                        || (rate != null && (!grade.equals(person.getGrade())
                                || !LocalDate.parse(c.getDate()).isAfter(today)));
                if (shown != null) total = total.add(new BigDecimal(shown));
                // The rate figure under it is bound; its late charge and floor keep following the sheet's today.
                c.bind(true, c.due(live));
                bound++;
            }
            claims.updateBinding(c);
        }
        boolean na = rates.find(sheetId, person.getGrade(), today) == null && !kept;
        BigDecimal cap = settings.cap(sheetId);
        if (cap != null && total.compareTo(cap) > 0) total = cap.setScale(2);
        person.stated(na ? null : total.toPlainString(), today.toString(), bound);
        people.updateStatements(person);
    }
}
