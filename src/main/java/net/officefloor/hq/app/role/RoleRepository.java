package net.officefloor.hq.app.role;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findBySheetIdOrderByUserNameAsc(String sheetId);

    Optional<Role> findBySheetIdAndUserName(String sheetId, String userName);

    /** The user name's role on the sheet: clerk unless one is set. */
    default String roleOf(String sheetId, String userName) {
        return findBySheetIdAndUserName(sheetId, userName).map(Role::getRole).orElse(Role.CLERK);
    }
}
