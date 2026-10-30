package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePoolMember {

    /** A share percent: at most two decimals. */
    private static final Pattern SHARE = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PoolMemberRequest request,
            Out<ValidPoolMember> body, Settings settings, Pools pools, People people)
            throws InvalidPoolException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String pool = request.pool() == null ? "" : request.pool();
        if (!pools.contains(sheetId, pool)) {
            throw new InvalidPoolException("No such pool");
        }
        if (pools.isCrew(sheetId, pool)) {
            throw new InvalidPoolException("A crew's members are its lead and crew members");
        }
        String memberPool = request.memberPool() == null ? "" : request.memberPool().trim();
        Long personId = null;
        String memberName;
        if (!memberPool.isEmpty()) {
            validateNesting(sheetId, pool, memberPool, pools);
            memberName = memberPool;
        } else {
            try {
                personId = Long.parseLong(request.personId() == null ? "" : request.personId().trim());
            } catch (NumberFormatException e) {
                throw new InvalidPoolException("Choose a person");
            }
            Person person = people.find(sheetId, personId);
            if (person == null) {
                throw new InvalidPoolException("No such person");
            }
            if (pools.hasMember(sheetId, pool, personId)) {
                throw new InvalidPoolException("The person is already in the pool");
            }
            memberName = person.getName();
        }
        String text = request.share() == null ? "" : request.share().trim();
        if (!SHARE.matcher(text).matches()) {
            throw new InvalidPoolException("A share is a percent with at most two decimals");
        }
        BigDecimal share = new BigDecimal(text).setScale(2);
        if (share.signum() <= 0 || share.compareTo(Pools.FULL) > 0) {
            throw new InvalidPoolException("A share is more than 0.00 and at most 100.00");
        }
        LocalDate from = date(request.from());
        LocalDate to = date(request.to());
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidPoolException("A to-date cannot be before the from-date");
        }
        body.set(new ValidPoolMember(pool, personId, memberPool.isEmpty() ? null : memberPool, memberName, share,
                from, to, request.user()));
    }

    /** The date (yyyy-MM-dd), or null when blank. */
    private static LocalDate date(String raw) throws InvalidPoolException {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidPoolException("A date is yyyy-MM-dd");
        }
    }

    /** Refuses the member pool when it is no pool of the sheet, is already a member of a pool, would contain the pool
     *  through some chain (or is the pool), or would make a chain of pools more than three deep. */
    private static void validateNesting(String sheetId, String pool, String memberPool, Pools pools)
            throws InvalidPoolException {
        if (!pools.contains(sheetId, memberPool)) {
            throw new InvalidPoolException("No such pool");
        }
        if (memberPool.equals(pool)) {
            throw new InvalidPoolException("A pool cannot contain itself");
        }
        if (pools.parentOf(sheetId, memberPool) != null) {
            throw new InvalidPoolException("The pool is already a member of a pool");
        }
        // The pool and the pools above it: none may be the member pool.
        int above = 0;
        Set<String> seen = new HashSet<>();
        for (String p = pool; p != null && seen.add(p); p = pools.parentOf(sheetId, p)) {
            if (p.equals(memberPool)) {
                throw new InvalidPoolException("A pool cannot contain itself");
            }
            above++;
        }
        if (above + height(sheetId, memberPool, pools, new HashSet<>()) > Pools.MAX_DEPTH) {
            throw new InvalidPoolException("A chain of pools is at most three deep");
        }
    }

    /** How deep the chain of pools from the pool down goes: 1 when no pool is a member of it. */
    private static int height(String sheetId, String pool, Pools pools, Set<String> seen) {
        if (!seen.add(pool)) {
            return Pools.MAX_DEPTH + 1;
        }
        int deepest = 0;
        for (String sub : pools.subPoolsOf(sheetId, pool)) {
            deepest = Math.max(deepest, height(sheetId, sub, pools, seen));
        }
        return 1 + deepest;
    }
}
