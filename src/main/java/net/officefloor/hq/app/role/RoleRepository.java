package net.officefloor.hq.app.role;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findBySheetIdOrderByUserNameAsc(String sheetId);

    Optional<Role> findBySheetIdAndUserName(String sheetId, String userName);

    /** The role the user name acts in on the sheet: steward while the sheet's today is inside a delegation to it,
     *  else its own role. */
    default String roleOf(String sheetId, String userName) {
        String own = ownRoleOf(sheetId, userName);
        return !Role.STEWARD.equals(own) && delegatorOf(sheetId, userName) != null ? Role.STEWARD : own;
    }

    /** The user name's own role on the sheet, leaving delegations aside: clerk unless one is set. */
    default String ownRoleOf(String sheetId, String userName) {
        return findBySheetIdAndUserName(sheetId, userName).map(Role::getRole).orElse(Role.CLERK);
    }

    /** The steward the user name acts for on the sheet's today (the latest delegation to it covering that day), or
     *  null when none. */
    default String delegatorOf(String sheetId, String userName) {
        List<String> by = activeDelegators(sheetId, userName);
        return by.isEmpty() ? null : by.get(0);
    }

    @Query("select d.byName from Delegation d, Sheet s where s.id = :sheetId and d.sheetId = :sheetId"
            + " and d.userName = :userName and d.fromDate <= s.today and d.toDate >= s.today order by d.id desc")
    List<String> activeDelegators(@Param("sheetId") String sheetId, @Param("userName") String userName);
}
