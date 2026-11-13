package net.officefloor.hq.app.asof;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.setting.DatedSettings;
import net.officefloor.hq.app.setting.DatedSettings.FromValue;

/**
 * A sheet's figures as they stood at a moment: its people, its claims (newest first), its rates, every dated setting
 * value recorded (by key, oldest first) and its policy date (null: blank).
 */
public record SheetState(List<Person> people, List<Claim> claims, List<Rate> rates,
        Map<String, List<FromValue>> settings, LocalDate policyDate) {

    /** A sheet with nothing on it yet. */
    public static final SheetState EMPTY = new SheetState(List.of(), List.of(), List.of(), Map.of(), null);

    /** The person with this id then, or null when they were not on the sheet. */
    public Person person(long id) {
        return people.stream().filter((p) -> p.id() == id).findFirst().orElse(null);
    }

    /** The person's claims then, newest first. */
    public List<Claim> claimsOf(long personId) {
        return claims.stream().filter((c) -> c.personId() == personId).toList();
    }

    /** The dated settings then. */
    public DatedSettings dated() {
        Map<String, NavigableMap<LocalDate, String>> values = new java.util.LinkedHashMap<>();
        settings.forEach((key, recorded) -> {
            NavigableMap<LocalDate, String> byDate = new TreeMap<>();
            recorded.forEach((v) -> byDate.put(v.from(), v.value()));
            values.put(key, byDate);
        });
        return new DatedSettings(values);
    }
}
