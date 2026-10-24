package net.officefloor.hq.app.person;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatronRepository extends JpaRepository<Patron, Long> {

    /** The sheet's patrons in the order they were named. */
    List<Patron> findBySheetIdOrderByIdAsc(String sheetId);

    /** The person's patrons in the order they were named. */
    List<Patron> findByPersonIdOrderByIdAsc(Long personId);

    /** Those naming the person as patron, in the order they named them. */
    List<Patron> findByPatronIdOrderByIdAsc(Long patronId);

    boolean existsByPersonIdAndPatronId(Long personId, Long patronId);
}
