package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds a person of the sheet to the pool with a share percent (at most two decimals, more than 0.00
 *  and at most 100.00): refused (nothing written) for no such pool or person, a person already in the
 *  pool, or a bad share. */
public class AddPoolMember {

    private static final Pattern SHARE = Pattern.compile("\\d+(\\.\\d{1,2})?");

    private static final BigDecimal MAX = new BigDecimal("100.00");

    public void add(@PathVariable("sheetId") String sheetId, @PathVariable("poolId") long poolId,
            @RequestParam(name = "by", required = false) String by, @RequestBody PoolMemberRequest request,
            PoolRepository pools, PersonRepository people, Out<PoolMemberAdded> added)
            throws InvalidPoolException {
        String id = sheetId.trim();
        String pool = pools.nameOf(id, poolId);
        if (pool == null) {
            throw new InvalidPoolException("No such pool: " + poolId);
        }
        if (request == null || request.personId() == null) {
            throw new InvalidPoolException("No person named");
        }
        Person person = people.find(id, request.personId());
        if (person == null) {
            throw new InvalidPoolException("No such person on the sheet: " + request.personId());
        }
        String text = request.share() == null ? "" : request.share().trim();
        if (!SHARE.matcher(text).matches()) {
            throw new InvalidPoolException("A share is a percent with at most two decimals");
        }
        BigDecimal share = new BigDecimal(text).setScale(2);
        if (share.signum() <= 0 || share.compareTo(MAX) > 0) {
            throw new InvalidPoolException("A share is more than 0.00 and at most 100.00");
        }
        if (pools.hasMember(poolId, person.getId())) {
            throw new InvalidPoolException(person.getName() + " is already in " + pool);
        }
        String user = by == null ? "" : by;
        pools.addMember(id, poolId, person.getId(), share, user);
        added.set(new PoolMemberAdded(id, pool, person.getName(), share.toPlainString(), user));
    }
}
