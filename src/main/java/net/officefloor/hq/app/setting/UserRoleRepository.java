package net.officefloor.hq.app.setting;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findBySheetIdOrderByUser(String sheetId);

    Optional<UserRole> findBySheetIdAndUser(String sheetId, String user);

    /** The user's role on the sheet: clerk unless set otherwise. */
    default String roleOf(String sheetId, String user) {
        return findBySheetIdAndUser(sheetId, user).map(UserRole::getRole).orElse(UserRole.CLERK);
    }
}
