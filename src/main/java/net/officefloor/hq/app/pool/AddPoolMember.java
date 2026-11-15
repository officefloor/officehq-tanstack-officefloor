package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds a person of the sheet, or another pool of the sheet, to the pool with a share percent (at most two
 *  decimals, more than 0.00 and at most 100.00): refused (nothing written) for no such pool or person, a
 *  person already in the pool, or a bad share; and for a pool that is already a member of a pool, that
 *  would contain itself through any chain, or that would make a chain of pools more than three deep. The
 *  membership may run from and/or to a date (optional); refused for a date that is not one, or a to-date
 *  before the from-date. */
public class AddPoolMember {

    private static final Pattern SHARE = Pattern.compile("\\d+(\\.\\d{1,2})?");

    private static final BigDecimal MAX = new BigDecimal("100.00");

    /** The most pools in a chain, the one a member of none counting as the first. */
    private static final int MAX_DEPTH = 3;

    public void add(@PathVariable("sheetId") String sheetId, @PathVariable("poolId") long poolId,
            @RequestParam(name = "by", required = false) String by, @RequestBody PoolMemberRequest request,
            PoolRepository pools, PersonRepository people, Out<PoolMemberAdded> added)
            throws InvalidPoolException {
        String id = sheetId.trim();
        String pool = pools.nameOf(id, poolId);
        if (pool == null) {
            throw new InvalidPoolException("No such pool: " + poolId);
        }
        if (request == null || (request.personId() == null && request.memberPoolId() == null)) {
            throw new InvalidPoolException("No member named");
        }
        Person person = null;
        String memberPool = null;
        if (request.personId() != null) {
            person = people.find(id, request.personId());
            if (person == null) {
                throw new InvalidPoolException("No such person on the sheet: " + request.personId());
            }
        } else {
            memberPool = pools.nameOf(id, request.memberPoolId());
            if (memberPool == null) {
                throw new InvalidPoolException("No such pool on the sheet: " + request.memberPoolId());
            }
        }
        String text = request.share() == null ? "" : request.share().trim();
        if (!SHARE.matcher(text).matches()) {
            throw new InvalidPoolException("A share is a percent with at most two decimals");
        }
        BigDecimal share = new BigDecimal(text).setScale(2);
        if (share.signum() <= 0 || share.compareTo(MAX) > 0) {
            throw new InvalidPoolException("A share is more than 0.00 and at most 100.00");
        }
        String from = date(request.from());
        String to = date(request.to());
        if (from != null && to != null && to.compareTo(from) < 0) {
            throw new InvalidPoolException("The membership ends before it starts: " + from + " to " + to);
        }
        String user = by == null ? "" : by;
        if (person != null) {
            if (pools.hasMember(poolId, person.getId())) {
                throw new InvalidPoolException(person.getName() + " is already in " + pool);
            }
            pools.addMember(id, poolId, person.getId(), share, from, to, user);
            added.set(new PoolMemberAdded(id, pool, person.getName(), share.toPlainString(), from, to, user));
            return;
        }
        long child = request.memberPoolId();
        int level = 0;
        for (Long above = poolId; above != null && level <= MAX_DEPTH; above = pools.parentOf(above)) {
            if (above == child) {
                throw new InvalidPoolException(memberPool + " cannot contain itself");
            }
            level++;
        }
        if (pools.parentOf(child) != null) {
            throw new InvalidPoolException(memberPool + " is already a member of a pool");
        }
        if (level + height(pools, child, 1) > MAX_DEPTH) {
            throw new InvalidPoolException("A chain of pools is at most " + MAX_DEPTH + " deep");
        }
        pools.addPoolMember(id, poolId, child, share, from, to, user);
        added.set(new PoolMemberAdded(id, pool, memberPool, share.toPlainString(), from, to, user));
    }

    /** The date as yyyy-mm-dd, or null when blank. */
    private static String date(String text) throws InvalidPoolException {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value).toString();
        } catch (DateTimeParseException e) {
            throw new InvalidPoolException("Not a date: " + value);
        }
    }

    /** The most pools in a chain down from {@code poolId}, itself counting as one (stops past the limit). */
    private static int height(PoolRepository pools, long poolId, int depth) {
        int most = 1;
        if (depth > MAX_DEPTH) return most;
        for (long below : pools.childrenOf(poolId)) {
            most = Math.max(most, 1 + height(pools, below, depth + 1));
        }
        return most;
    }
}
