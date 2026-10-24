package net.officefloor.hq.app.person;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonAliasRepository extends JpaRepository<PersonAlias, Long> {

    /** The sheet's aliases in the order they were added. */
    List<PersonAlias> findBySheetIdOrderByIdAsc(String sheetId);

    /** The person's aliases in the order they were added. */
    List<PersonAlias> findByPersonIdOrderByIdAsc(Long personId);

    /** Whether the alias is in use on the sheet, ignoring case. */
    boolean existsBySheetIdAndAliasIgnoreCase(String sheetId, String alias);

    /** The aliases on any of the sheets. */
    List<PersonAlias> findBySheetIdIn(java.util.Collection<String> sheetIds);

    Optional<PersonAlias> findFirstBySheetIdAndAliasIgnoreCase(String sheetId, String alias);

    /** The person on the sheet going by the name or alias, ignoring case. */
    static Optional<Person> findByNameOrAlias(List<Person> onSheet, PersonAliasRepository aliases, String sheetId,
            String name) {
        Optional<Person> named = onSheet.stream().filter(p -> p.getName().equalsIgnoreCase(name)).findFirst();
        if (named.isPresent()) {
            return named;
        }
        return aliases.findFirstBySheetIdAndAliasIgnoreCase(sheetId, name)
                .flatMap(a -> onSheet.stream().filter(p -> p.getId().equals(a.getPersonId())).findFirst());
    }
}
