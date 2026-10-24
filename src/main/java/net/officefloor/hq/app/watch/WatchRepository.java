package net.officefloor.hq.app.watch;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchRepository extends JpaRepository<Watch, Long> {

    /** The person's watchers in the order they started watching. */
    List<Watch> findByPersonIdOrderByIdAsc(Long personId);

    /** What the user watches on the sheet. */
    List<Watch> findBySheetIdAndUserNameOrderByIdAsc(String sheetId, String userName);

    Optional<Watch> findByPersonIdAndUserName(Long personId, String userName);
}
