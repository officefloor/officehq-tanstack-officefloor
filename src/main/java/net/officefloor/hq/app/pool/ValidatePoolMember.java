package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a member for a pool the sheet does not have, a person not on the sheet or already in the
 *  pool (a crew's pool takes its members from the crew), a member pool (or crew) the sheet does not have, already a member of a pool, that would contain itself
 *  or that would make a chain of pools more than three deep, or a share that is not a percent with up to two decimals, more than 0.00 and at most 100.00, or a
 *  from- or to-date that is not a date, or a to-date before the from-date. */
public class ValidatePoolMember {

    private static final Pattern SHARE = Pattern.compile("\\d{1,3}(\\.\\d{1,2})?");
    private static final BigDecimal MAX = new BigDecimal("100.00");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PoolMemberRequest request,
            Pools pools, Out<PoolMember> member, Out<Actor> actor) throws InvalidSheetRequestException {
        actor.set(new Actor(request == null ? null : request.user()));
        if (request == null || request.pool() == null || !pools.has(sheetId, request.pool())) {
            throw new InvalidSheetRequestException("No such pool");
        }
        String memberPool = request.memberPool() == null || request.memberPool().isBlank() ? null
                : request.memberPool().trim();
        String name;
        if (memberPool != null) {
            if (!pools.has(sheetId, memberPool) && !pools.hasCrew(sheetId, memberPool)) {
                throw new InvalidSheetRequestException("No such pool: " + memberPool);
            }
            name = memberPool;
        } else {
            name = request.personId() == null ? null : pools.personName(sheetId, request.personId());
            if (name == null) {
                throw new InvalidSheetRequestException("No such person");
            }
        }
        String text = request.share() == null ? "" : request.share().trim();
        if (!SHARE.matcher(text).matches()) {
            throw new InvalidSheetRequestException("A share is a percent with up to two decimals");
        }
        BigDecimal share = new BigDecimal(text).setScale(2);
        if (share.signum() <= 0 || share.compareTo(MAX) > 0) {
            throw new InvalidSheetRequestException("A share is more than 0.00 and at most 100.00");
        }
        LocalDate from = date(request.from());
        LocalDate to = date(request.to());
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidSheetRequestException("A to-date cannot be before the from-date");
        }
        if (memberPool != null) {
            validatePool(sheetId, request.pool(), memberPool, pools);
            member.set(new PoolMember(sheetId, request.pool(), null, memberPool, name, share, request.user(), from, to));
            return;
        }
        if (pools.hasMember(sheetId, request.pool(), request.personId())) {
            throw new InvalidSheetRequestException(name + " is already in " + request.pool());
        }
        member.set(new PoolMember(sheetId, request.pool(), request.personId(), null, name, share, request.user(), from, to));
    }

    /** The date (yyyy-mm-dd), or null when blank. */
    private static LocalDate date(String raw) throws InvalidSheetRequestException {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidSheetRequestException("Not a date (yyyy-mm-dd): " + raw);
        }
    }

    /** Refuses a pool already a member of a pool, one that would contain itself through any chain, and
     *  one that would make a chain of pools more than {@link Pools#MAX_DEPTH} deep. */
    private static void validatePool(String sheetId, String pool, String memberPool, Pools pools)
            throws InvalidSheetRequestException {
        if (pools.parentOf(sheetId, memberPool) != null) {
            throw new InvalidSheetRequestException(memberPool + " is already a member of a pool");
        }
        // The pools above, the pool itself among them.
        int above = 0;
        for (String p = pool; p != null && above <= Pools.MAX_DEPTH; p = pools.parentOf(sheetId, p)) {
            if (p.equals(memberPool)) {
                throw new InvalidSheetRequestException(memberPool + " cannot contain itself");
            }
            above++;
        }
        if (above + height(sheetId, memberPool, pools, 0) > Pools.MAX_DEPTH) {
            throw new InvalidSheetRequestException("A chain of pools is at most " + Pools.MAX_DEPTH + " deep");
        }
    }

    /** How many pools deep the chain below the pool runs, the pool itself counted. */
    private static int height(String sheetId, String pool, Pools pools, int depth) {
        if (depth > Pools.MAX_DEPTH) return depth;
        int most = 0;
        for (String m : pools.memberPools(sheetId, pool)) most = Math.max(most, height(sheetId, m, pools, depth + 1));
        return 1 + most;
    }
}
