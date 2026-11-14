package net.officefloor.hq.app.rollup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.Dues;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** The sheet's figures at one moment, as the screen shows them: each person's claimed and
 *  due-total (null: n/a), in the order they were added, and the sheet's claimed-total and due-total. */
public record Rollup(Map<Long, Figures> people, String claimedTotal, String dueTotal) {

    /** A person's figures: their name, claimed and due-total (null: n/a). */
    public record Figures(String name, String claimed, String dueTotal) {
        public boolean sameAs(Figures other) {
            return other != null && claimed.equals(other.claimed)
                    && (dueTotal == null ? other.dueTotal == null : dueTotal.equals(other.dueTotal));
        }
    }

    /** The sheet's figures now. */
    public static Rollup of(String sheetId, PersonRepository people, ClaimRepository claims,
            RateRepository rates, SheetRepository sheets, SettingRepository settings) {
        List<Rate> sheetRates = rates.list(sheetId);
        String floor = settings.floor(sheetId);
        LocalDate todayDate = sheets.today(sheetId);
        String today = todayDate.toString();
        Dues.Late late = settings.late(sheetId, todayDate);
        List<Claim> all = claims.list(sheetId);
        Map<Long, Figures> figures = new LinkedHashMap<>();
        BigDecimal claimedTotal = zero();
        BigDecimal dueTotal = zero();
        for (Person p : people.list(sheetId)) {
            BigDecimal claimed = zero();
            BigDecimal due = zero();
            boolean kept = false;
            for (Claim c : all) {
                if (c.getPersonId() != p.getId() || !p.countsLive(c)) continue;
                claimed = claimed.add(new BigDecimal(c.getAmount()));
                String grade = p.gradeOn(c.getDate());
                String pct = rateOn(sheetRates, grade, c.getDate());
                String d = Dues.shown(c, pct == null ? null : Dues.of(c.getAmount(), pct, floor, late.rounding(c.getRoundDate())), late);
                kept |= c.getSettledDue() != null || (c.isStated() && c.getStatedDue() != null)
                        || (pct != null && !grade.equals(p.getGrade()));
                if (d != null) due = due.add(new BigDecimal(d));
            }
            boolean na = rateOn(sheetRates, p.getGrade(), today) == null && !kept;
            figures.put(p.getId(), new Figures(p.getName(), claimed.toPlainString(),
                    na ? null : due.toPlainString()));
            claimedTotal = claimedTotal.add(claimed);
            if (!na) dueTotal = dueTotal.add(due);
        }
        return new Rollup(figures, claimedTotal.toPlainString(), dueTotal.toPlainString());
    }

    /** The audit lines after a change from {@code before} to this: one ROLLUP per person whose
     *  figures moved (a new person counts as moved), then TOTALS when a sheet total moved. */
    public List<String> changesSince(Rollup before, String sheetId, String by) {
        String head = " sheet=" + sheetId + " by=" + by;
        List<String> lines = new ArrayList<>();
        people.forEach((id, now) -> {
            if (!now.sameAs(before.people.get(id))) {
                lines.add("ROLLUP" + head + " person=" + now.name() + " claimed=" + now.claimed()
                        + " due-total=" + (now.dueTotal() == null ? "n/a" : now.dueTotal()));
            }
        });
        if (!claimedTotal.equals(before.claimedTotal) || !dueTotal.equals(before.dueTotal)) {
            lines.add("TOTALS" + head + " claimed-total=" + claimedTotal + " due-total=" + dueTotal);
        }
        return lines;
    }

    /** The grade's rate in force on the date (yyyy-MM-dd): the one with the latest from-date on or
     *  before it; null when there is none. */
    private static String rateOn(List<Rate> rates, String grade, String date) {
        Rate found = null;
        for (Rate r : rates) {
            if (!r.grade().equals(grade) || (r.from() != null && r.from().compareTo(date) > 0)) continue;
            if (found == null || nz(found.from()).compareTo(nz(r.from())) < 0) found = r;
        }
        return found == null ? null : found.pct();
    }

    private static String nz(String from) {
        return from == null ? "" : from;
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2);
    }
}
