package net.officefloor.hq.app.person;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonRepository extends JpaRepository<Person, Long> {

    /** The sheet's people in list order: by their place, a person added later at the end. */
    @Query("select p from Person p where p.sheetId = :sheetId order by coalesce(p.position, p.id), p.id")
    List<Person> findListed(@Param("sheetId") String sheetId);

    /** Sets a person's place in the list, leaving their version as it is: a move is no edit of them. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Person p set p.position = :position where p.id = :id")
    void placeAt(@Param("id") Long id, @Param("position") long position);

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
