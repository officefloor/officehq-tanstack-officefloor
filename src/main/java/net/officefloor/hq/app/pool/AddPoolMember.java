package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Adds a person to a pool with a share percent (at most two decimals, above 0.00 and at most 100.00); refused for an
 * unknown pool or person, a person already in the pool, or a bad share.
 */
public class AddPoolMember {

    private static final Pattern SHARE = Pattern.compile("\\d{1,3}(\\.\\d{1,2})?");

    private static final BigDecimal FULL = new BigDecimal("100.00");

    public void add(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody PoolMemberRequest request, Pools pools, People people, Audit audit)
            throws InvalidPoolException {
        if (request == null || request.pool() == null || !pools.has(sheet, request.pool())) {
            throw new InvalidPoolException("No such pool");
        }
        Person person = request.person() == null ? null : people.find(sheet, request.person());
        if (person == null) {
            throw new InvalidPoolException("Choose a person");
        }
        String raw = request.share() == null ? "" : request.share().trim();
        if (!SHARE.matcher(raw).matches()) {
            throw new InvalidPoolException("A share is a percent with at most two decimals");
        }
        BigDecimal share = new BigDecimal(raw).setScale(2);
        if (share.signum() <= 0 || share.compareTo(FULL) > 0) {
            throw new InvalidPoolException("A share must be more than 0.00 and at most 100.00");
        }
        if (pools.isMember(sheet, request.pool(), person.id())) {
            throw new InvalidPoolException(person.name() + " is already in " + request.pool());
        }
        pools.addMember(sheet, request.pool(), person.id(), share);
        audit.record("POOL_MEMBER_ADDED sheet=" + sheet + " by=" + user + " pool=" + request.pool() + " name="
                + person.name() + " share=" + share.toPlainString());
    }
}
