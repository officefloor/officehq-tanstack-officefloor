package net.officefloor.hq.app.sheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Late;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.setting.Dated;
import net.officefloor.hq.app.setting.DatedValue;
import net.officefloor.hq.app.setting.Settings;

/**
 * What a sheet holds at one moment that its figures are made from: its people, claims (with their status logs) and
 * rates, the recorded values of its floor, late percent and cap, and its policy date ("" when none is set). Read
 * {@link #on} a date, it stands as it would with that date for the sheet's today.
 */
public record SheetState(List<Person> people, List<Claim> claims, List<Rate> rates, List<DatedValue> floor,
        List<DatedValue> latePct, List<DatedValue> cap, String policy) {

    /** The person with the id as they stand with the date for the sheet's today; null when they were not on the sheet. */
    public Person person(long id, LocalDate today) {
        return people.stream().filter((p) -> p.id() == id).findFirst().map((p) -> p.on(today)).orElse(null);
    }

    /** The person's claims as they stand with the date for the sheet's today, newest first. */
    public List<Claim> claimsOf(long person, LocalDate today) {
        return claims.stream().filter((c) -> c.person() == person).map((c) -> c.on(today)).toList();
    }

    /** The floor by date. */
    public Dated floorDated() {
        return Settings.figure(floor, new BigDecimal(Settings.DEFAULT_FLOOR));
    }

    /** What makes the late charges with the date for the sheet's today. */
    public Late late(LocalDate today) {
        return new Late(Settings.figure(latePct, new BigDecimal(Settings.DEFAULT_LATE_PCT)), today,
                policy.isEmpty() ? today : LocalDate.parse(policy));
    }

    /** The cap in force on the date, or null when there is none. */
    public BigDecimal capOn(LocalDate today) {
        String value = Settings.on(cap, today);
        return value.isEmpty() ? null : new BigDecimal(value);
    }
}
