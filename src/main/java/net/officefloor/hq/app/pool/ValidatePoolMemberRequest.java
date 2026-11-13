package net.officefloor.hq.app.pool;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Refuses (400) a member for a pool not on the sheet, a person not on the sheet or already in the pool, a pool not on
 * the sheet, already a member of a pool, the pool itself or one containing it through any chain, or one that would make
 * a chain of pools more than three deep, or a share that is not a number with at most two decimals, more than 0.00 and
 * at most 100.00.
 */
public class ValidatePoolMemberRequest {

    private static final Pattern SHARE = Pattern.compile("\\d{1,3}(\\.\\d{1,2})?");

    private static final int DEEPEST = 3;

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public void validate(@Val Sheet sheet, @PathVariable("poolId") String poolId,
            @RequestBody @Valid PoolMemberRequest request, PoolRepository pools, PersonRepository people,
            Out<NewPoolMember> added) throws InvalidPoolException {
        Pool pool;
        try {
            pool = pools.find(sheet.id(), Long.parseLong(poolId));
        } catch (NumberFormatException e) {
            pool = null;
        }
        if (pool == null) {
            throw new InvalidPoolException("That pool is not on the sheet.");
        }
        PoolMember member = request != null && request.poolId() != null && request.personId() == null
                ? poolMember(sheet, pool, request.poolId(), pools)
                : null;
        Person person = member != null || request == null || request.personId() == null ? null
                : people.find(sheet.id(), request.personId());
        if (member == null && person == null) {
            throw new InvalidPoolException("Choose a person or a pool on the sheet.");
        }
        if (person != null && pool.members().stream().anyMatch(m -> m.personId() != null && m.personId() == person.id())) {
            throw new InvalidPoolException(person.name() + " is already in the pool.");
        }
        String text = request.share() == null ? "" : request.share().trim();
        if (!SHARE.matcher(text).matches()) {
            throw new InvalidPoolException("A share is a percent with at most two decimals.");
        }
        BigDecimal share = new BigDecimal(text).setScale(2);
        if (share.signum() <= 0 || share.compareTo(HUNDRED) > 0) {
            throw new InvalidPoolException("A share is more than 0.00 and at most 100.00.");
        }
        added.set(new NewPoolMember(pool, member != null ? new PoolMember(null, member.poolId(), member.name(), share, false)
                : new PoolMember(person.id(), null, person.name(), share, person.retiredFrom() != null)));
    }

    /** The pool {@code memberId} as a member of {@code pool} (share unset), refused when it may not be one. */
    private static PoolMember poolMember(Sheet sheet, Pool pool, long memberId, PoolRepository repository)
            throws InvalidPoolException {
        Pools pools = new Pools(repository.findAll(sheet.id()), null);
        Pool member = pools.find(memberId);
        if (member == null) {
            throw new InvalidPoolException("That pool is not on the sheet.");
        }
        if (pools.within(pool.id(), memberId)) {
            throw new InvalidPoolException(member.name() + " cannot contain itself.");
        }
        if (pools.parentOf(memberId) != null) {
            throw new InvalidPoolException(member.name() + " is already a member of a pool.");
        }
        if (pools.level(pool.id()) + pools.height(memberId) > DEEPEST) {
            throw new InvalidPoolException("A chain of pools is at most " + DEEPEST + " deep.");
        }
        return new PoolMember(null, memberId, member.name(), null, false);
    }
}
