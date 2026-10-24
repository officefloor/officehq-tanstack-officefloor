package net.officefloor.hq.app.person;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {

    /** The sheet's people in the order they were added. */
    List<Person> findBySheetIdOrderByIdAsc(String sheetId);

    Optional<Person> findByIdAndSheetId(Long id, String sheetId);

    /** Whether the name is in use on the sheet, ignoring case. */
    boolean existsBySheetIdAndNameIgnoreCase(String sheetId, String name);

    /** Whether the name is in use on the sheet by someone other than this person, ignoring case. */
    boolean existsBySheetIdAndNameIgnoreCaseAndIdNot(String sheetId, String name, Long id);

    /** Whether the contact is someone's on the sheet. */
    boolean existsBySheetIdAndContact(String sheetId, String contact);

    /** Whether the contact is on the sheet for someone other than this person. */
    boolean existsBySheetIdAndContactAndIdNot(String sheetId, String contact, Long id);

    /** The people on any of the sheets. */
    List<Person> findBySheetIdIn(java.util.Collection<String> sheetIds);
}
