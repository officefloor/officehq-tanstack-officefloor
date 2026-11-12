package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a member by a viewer, of a pool or person not on the sheet, of a person already in the pool, or
 *  with a share that is not a number of at most two decimals, more than 0.00 and at most 100.00. A member pool
 *  is refused when it is already a member of a pool, when it would contain the pool through any chain (or is
 *  the pool itself), or when the chain of pools would be more than three deep. A from- or to-date is optional;
 *  one that is not a date, or a to-date before the from-date, is refused. */
public class ValidateMember {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final int MAX_DEPTH = 3;

    public void validate(@PathVariable("sheetId") String sheetId, @PathVariable("poolId") String poolId,
            @RequestBody MemberRequest request, Out<MemberInput> body, RoleStore roles, PoolStore pools,
            PersonStore people) throws InvalidSheetException {
        String sheet = sheetId.trim();
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheet, user);
        PoolStore.Pool pool;
        try {
            pool = pools.find(sheet, Long.parseLong(poolId.trim()));
        } catch (NumberFormatException e) {
            pool = null;
        }
        if (pool == null) {
            throw new InvalidSheetException("No such pool");
        }
        PoolStore.Pool member = request == null || request.memberPoolId() == null ? null
                : pools.find(sheet, request.memberPoolId());
        if (request != null && request.memberPoolId() != null && member == null) {
            throw new InvalidSheetException("No such pool to add");
        }
        Person person = member != null || request == null || request.personId() == null ? null
                : people.find(sheet, request.personId());
        if (member == null && person == null) {
            throw new InvalidSheetException("A person is required");
        }
        String text = request.share() == null ? "" : request.share().trim();
        if (!text.matches("\\d+(\\.\\d{1,2})?")) {
            throw new InvalidSheetException("A share is a percent with at most two decimals");
        }
        BigDecimal share = new BigDecimal(text).setScale(2);
        if (share.signum() <= 0 || share.compareTo(HUNDRED) > 0) {
            throw new InvalidSheetException("A share is more than 0.00 and at most 100.00");
        }
        LocalDate from = date(request.from());
        LocalDate to = date(request.to());
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidSheetException("The to date must not be before the from date");
        }
        if (member != null) {
            refuseNesting(sheet, pool, member, pools);
            body.set(new MemberInput(pool, null, member.id(), member.name(), share, from, to, user));
            return;
        }
        if (pools.isMember(pool.id(), person.getId())) {
            throw new InvalidSheetException(person.getName() + " is already in " + pool.name());
        }
        body.set(new MemberInput(pool, person.getId(), null, person.getName(), share, from, to, user));
    }

    /** The date given, or null when blank. */
    private static LocalDate date(String value) throws InvalidSheetException {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + text);
        }
    }

    private static void refuseNesting(String sheet, PoolStore.Pool pool, PoolStore.Pool member, PoolStore pools)
            throws InvalidSheetException {
        if (member.id() == pool.id()) {
            throw new InvalidSheetException(pool.name() + " cannot be a member of itself");
        }
        Map<Long, Long> parents = pools.parents(sheet);
        if (parents.containsKey(member.id())) {
            throw new InvalidSheetException(member.name() + " is already a member of a pool");
        }
        // the pools above the pool, counting it
        int above = 0;
        for (Long at = pool.id(); at != null; at = parents.get(at)) {
            if (at == member.id()) {
                throw new InvalidSheetException(member.name() + " already contains " + pool.name());
            }
            above++;
        }
        if (above + depth(member.id(), parents) > MAX_DEPTH) {
            throw new InvalidSheetException("A chain of pools is at most " + MAX_DEPTH + " deep");
        }
    }

    /** How deep the chain of pools from {@code poolId} down goes, counting it. */
    private static int depth(long poolId, Map<Long, Long> parents) {
        int deepest = 0;
        for (Map.Entry<Long, Long> e : parents.entrySet()) {
            if (e.getValue() == poolId) {
                deepest = Math.max(deepest, depth(e.getKey(), parents));
            }
        }
        return deepest + 1;
    }
}
