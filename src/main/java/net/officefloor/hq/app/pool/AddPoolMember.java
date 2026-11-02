package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Adds a member to a pool, a person or another pool, with a share percent (at most two decimals, above 0.00 and at
 * most 100.00); refused for an unknown pool or person, a person already in the pool, or a bad share. A pool member is
 * also refused when it is already a member of a pool, when the pool would contain itself through any chain, or when
 * the chain of pools would be more than three deep. The membership may carry a from-date and a to-date (each optional);
 * a to-date before the from-date is refused. A crew's pool takes no members this way: its members are the crew's.
 */
public class AddPoolMember {

    private static final Pattern SHARE = Pattern.compile("\\d{1,3}(\\.\\d{1,2})?");

    private static final int MAX_DEPTH = 3;

    private static final BigDecimal FULL = new BigDecimal("100.00");

    public void add(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody PoolMemberRequest request, Pools pools, People people, Audit audit,
            Rollup rollup) throws InvalidPoolException {
        if (request == null || request.pool() == null || !pools.has(sheet, request.pool())) {
            throw new InvalidPoolException("No such pool");
        }
        if (pools.isCrew(sheet, request.pool())) {
            throw new InvalidPoolException(request.pool() + " is a crew: its members are the crew's");
        }
        String sub = request.memberPool() == null ? null : request.memberPool().trim();
        Person person = sub != null || request.person() == null ? null : people.find(sheet, request.person());
        if (sub != null ? !pools.has(sheet, sub) : person == null) {
            throw new InvalidPoolException("Choose a person or a pool");
        }
        String raw = request.share() == null ? "" : request.share().trim();
        if (!SHARE.matcher(raw).matches()) {
            throw new InvalidPoolException("A share is a percent with at most two decimals");
        }
        BigDecimal share = new BigDecimal(raw).setScale(2);
        if (share.signum() <= 0 || share.compareTo(FULL) > 0) {
            throw new InvalidPoolException("A share must be more than 0.00 and at most 100.00");
        }
        LocalDate from = date(request.from());
        LocalDate to = date(request.to());
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidPoolException("A membership may not end before it starts");
        }
        String dates = (from == null ? "" : " from=" + from) + (to == null ? "" : " to=" + to);
        if (sub != null) {
            checkSubPool(sheet, request.pool(), sub, pools);
            Rollup.Snapshot before = rollup.take(sheet);
            pools.addSubPool(sheet, request.pool(), sub, share, from, to);
            audit.record("POOL_MEMBER_ADDED sheet=" + sheet + " by=" + user + " pool=" + request.pool() + " name="
                    + sub + " share=" + share.toPlainString() + dates);
            // The cuts it moves move the capped due-totals.
            rollup.record(sheet, user, before);
            return;
        }
        if (pools.isMember(sheet, request.pool(), person.id())) {
            throw new InvalidPoolException(person.name() + " is already in " + request.pool());
        }
        Rollup.Snapshot before = rollup.take(sheet);
        pools.addMember(sheet, request.pool(), person.id(), share, from, to);
        audit.record("POOL_MEMBER_ADDED sheet=" + sheet + " by=" + user + " pool=" + request.pool() + " name="
                + person.name() + " share=" + share.toPlainString() + dates);
        rollup.record(sheet, user, before);
    }

    /** The date given, or null when blank. */
    private static LocalDate date(String raw) throws InvalidPoolException {
        String date = raw == null ? "" : raw.trim();
        if (date.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new InvalidPoolException("Not a valid date: " + date);
        }
    }

    /** Refuses a pool already in a pool, one that would contain itself, or a chain more than three deep. */
    private static void checkSubPool(String sheet, String pool, String sub, Pools pools) throws InvalidPoolException {
        if (pools.parentOf(sheet, sub) != null) {
            throw new InvalidPoolException(sub + " is already a member of a pool");
        }
        int above = 0;
        for (String p = pool; p != null; p = pools.parentOf(sheet, p)) {
            if (p.equals(sub)) {
                throw new InvalidPoolException(pool + " cannot contain itself");
            }
            above++;
        }
        if (above + depth(sheet, sub, pools) > MAX_DEPTH) {
            throw new InvalidPoolException("A chain of pools is at most " + MAX_DEPTH + " deep");
        }
    }

    /** How many pools deep the chain below and including this pool goes. */
    private static int depth(String sheet, String pool, Pools pools) {
        int below = 0;
        for (String sub : pools.subPools(sheet, pool)) {
            below = Math.max(below, depth(sheet, sub, pools));
        }
        return below + 1;
    }
}
