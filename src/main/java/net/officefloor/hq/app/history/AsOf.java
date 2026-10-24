package net.officefloor.hq.app.history;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLog;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Patron;
import net.officefloor.hq.app.person.PatronRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonAlias;
import net.officefloor.hq.app.person.PersonAliasRepository;
import net.officefloor.hq.app.person.PersonGrade;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateLogRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Delegation;
import net.officefloor.hq.app.role.DelegationRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.InvalidTodayException;
import net.officefloor.hq.app.sheet.SettingLogRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.statement.Statement;
import net.officefloor.hq.app.statement.StatementRepository;
import net.officefloor.hq.app.watch.Watch;
import net.officefloor.hq.app.watch.WatchRepository;
import org.springframework.stereotype.Service;

/**
 * A sheet as it stood at the end of a date (an as-of view): every change made while the sheet's today was on or
 * before it counts, every later one does not, and that date stands in for the sheet's today. With no date, each
 * read gives the sheet as the store holds it now. Only worked out, never saved.
 */
@Service
public class AsOf {

    private final History history;
    private final SettingLogRepository settingLogs;
    private final RateLogRepository rateLogs;
    private final RateRepository rates;
    private final PersonRepository people;
    private final PersonGradeRepository grades;
    private final PatronRepository patrons;
    private final PersonAliasRepository aliases;
    private final StatementRepository statements;
    private final ClaimRepository claims;
    private final ClaimLogRepository claimLogs;
    private final PeriodRepository periods;
    private final RoleRepository roles;
    private final DelegationRepository delegations;
    private final WatchRepository watches;

    public AsOf(History history, SettingLogRepository settingLogs, RateLogRepository rateLogs, RateRepository rates,
            PersonRepository people, PersonGradeRepository grades, PatronRepository patrons,
            PersonAliasRepository aliases, StatementRepository statements, ClaimRepository claims, ClaimLogRepository claimLogs,
            PeriodRepository periods, RoleRepository roles, DelegationRepository delegations, WatchRepository watches) {
        this.history = history;
        this.settingLogs = settingLogs;
        this.rateLogs = rateLogs;
        this.rates = rates;
        this.people = people;
        this.grades = grades;
        this.patrons = patrons;
        this.aliases = aliases;
        this.statements = statements;
        this.claims = claims;
        this.claimLogs = claimLogs;
        this.periods = periods;
        this.roles = roles;
        this.delegations = delegations;
        this.watches = watches;
    }

    /** The as-of date a read asks for: null when none is given; refuses one that is not a calendar date. */
    public static LocalDate dateOf(String raw) throws InvalidTodayException {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidTodayException(raw.trim());
        }
    }

    /** The sheet as of the date: its settings then, archived only if it was by then, the date its today. */
    public Sheet sheet(Sheet sheet, LocalDate date) {
        return date == null ? sheet : settingLogs.sheetAsOf(sheet, date);
    }

    /** The sheet's people, in list order: as of a date, those then on the sheet in the places they hold now. */
    public List<Person> people(String sheetId, LocalDate date) {
        List<Person> now = people.findListed(sheetId);
        if (date == null) {
            return now;
        }
        Map<Long, Long> places = now.stream().collect(Collectors.toMap(Person::getId, Person::getPlace));
        return sorted(history.asOf(sheetId, Person.class, date, now, Person::getId),
                Comparator.<Person>comparingLong(p -> places.getOrDefault(p.getId(), p.getPlace())).thenComparing(Person::getId));
    }

    /** The sheet's dated grades of each person, by from-date, by person id. */
    public Map<Long, List<PersonGrade>> grades(String sheetId, LocalDate date) {
        if (date == null) {
            return grades.datedOnSheet(sheetId);
        }
        return sorted(history.asOf(sheetId, PersonGrade.class, date, grades.findBySheetIdOrderByFromDateAsc(sheetId),
                PersonGrade::getId), Comparator.comparing(PersonGrade::getFromDate)).stream()
                .collect(Collectors.groupingBy(PersonGrade::getPersonId));
    }

    /** The sheet's patrons, in the order they were named. */
    public List<Patron> patrons(String sheetId, LocalDate date) {
        List<Patron> now = patrons.findBySheetIdOrderByIdAsc(sheetId);
        return date == null ? now
                : sorted(history.asOf(sheetId, Patron.class, date, now, Patron::getId), Comparator.comparing(Patron::getId));
    }

    /** The sheet's aliases, in the order they were given. */
    public List<PersonAlias> aliases(String sheetId, LocalDate date) {
        List<PersonAlias> now = aliases.findBySheetIdOrderByIdAsc(sheetId);
        return date == null ? now
                : sorted(history.asOf(sheetId, PersonAlias.class, date, now, PersonAlias::getId),
                        Comparator.comparing(PersonAlias::getId));
    }

    /** The sheet's statements, oldest first. */
    public List<Statement> statements(String sheetId, LocalDate date) {
        List<Statement> now = statements.findBySheetIdOrderByIdAsc(sheetId);
        return date == null ? now
                : sorted(history.asOf(sheetId, Statement.class, date, now, Statement::getId),
                        Comparator.comparing(Statement::getId));
    }

    /** The sheet's claims in the order they are listed: newest first, a moved claim as its new person's newest. */
    public List<Claim> claims(String sheetId, LocalDate date) {
        List<Claim> now = claims.findBySheetIdOrderByListedDescIdDesc(sheetId);
        return date == null ? now
                : sorted(history.asOf(sheetId, Claim.class, date, now, Claim::getId),
                        Comparator.comparing(Claim::getListed, Comparator.nullsFirst(Comparator.naturalOrder()))
                                .thenComparing(Claim::getId).reversed());
    }

    /** The status log of each claim on the sheet as the screen shows it, oldest first, by claim id: only the lines
     *  written while the sheet's today was on or before the date. */
    public Map<Long, List<String>> claimLines(String sheetId, LocalDate date) {
        if (date == null) {
            return claimLogs.linesOnSheet(sheetId);
        }
        return claimLogs.findBySheetIdOrderByIdAsc(sheetId).stream().filter(l -> !l.getOnDate().isAfter(date))
                .collect(Collectors.groupingBy(ClaimLog::getClaimId,
                        Collectors.mapping(ClaimLog::line, Collectors.toList())));
    }

    /** The sheet's periods, by from-date. */
    public List<Period> periods(String sheetId, LocalDate date) {
        List<Period> now = periods.findBySheetIdOrderByFromDateAscIdAsc(sheetId);
        return date == null ? now
                : sorted(history.asOf(sheetId, Period.class, date, now, Period::getId),
                        Comparator.comparing(Period::getFromDate).thenComparing(Period::getId));
    }

    /** The sheet's rates, by grade, then from-date (one from the beginning of time first); then, in turn, those of each
     *  sheet up its chain of parents (each as of the date too), as {@link RateRepository#findInChain}. */
    public List<Rate> rates(String sheetId, LocalDate date) {
        if (date == null) {
            return rates.findInChain(sheetId);
        }
        return rates.chainOf(sheetId).stream()
                .flatMap(id -> sorted(rateLogs.ratesAsOf(id, date), Comparator.comparing(Rate::getGrade)
                        .thenComparing(Rate::getFromDate, Comparator.nullsFirst(Comparator.naturalOrder()))).stream())
                .toList();
    }

    /** The user names given a role on the sheet, by name. */
    public List<Role> roles(String sheetId, LocalDate date) {
        List<Role> now = roles.findBySheetIdOrderByUserNameAsc(sheetId);
        return date == null ? now
                : sorted(history.asOf(sheetId, Role.class, date, now, Role::getId), Comparator.comparing(Role::getUserName));
    }

    /** The steward role delegations made on the sheet, oldest first. */
    public List<Delegation> delegations(String sheetId, LocalDate date) {
        List<Delegation> now = delegations.findBySheetIdOrderByIdAsc(sheetId);
        return date == null ? now
                : sorted(history.asOf(sheetId, Delegation.class, date, now, Delegation::getId),
                        Comparator.comparing(Delegation::getId));
    }

    /** The user's watches on the sheet, in the order made. */
    public List<Watch> watches(String sheetId, String user, LocalDate date) {
        List<Watch> now = watches.findBySheetIdAndUserNameOrderByIdAsc(sheetId, user);
        return date == null ? now
                : sorted(history.asOf(sheetId, Watch.class, date, now, Watch::getId), Comparator.comparing(Watch::getId))
                        .stream().filter(w -> user.equals(w.getUserName())).toList();
    }

    private static <T> List<T> sorted(List<T> list, Comparator<T> order) {
        return list.stream().sorted(order).toList();
    }
}
