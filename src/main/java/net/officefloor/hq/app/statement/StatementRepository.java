package net.officefloor.hq.app.statement;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatementRepository extends JpaRepository<Statement, Long> {

    /** The sheet's statements, by id. */
    List<Statement> findBySheetIdOrderByIdAsc(String sheetId);

    /** The sheet's statements, each person's in the order they stand, oldest first. */
    default List<Statement> ofSheet(String sheetId) {
        return findBySheetIdOrderByIdAsc(sheetId).stream().sorted(Statement.ORDER).toList();
    }

    /** The person's statements, by id. */
    List<Statement> findByPersonIdOrderByIdAsc(Long personId);

    /** The person's statements in the order they stand, oldest first: the last is their last statement. */
    default List<Statement> ofPerson(Long personId) {
        return findByPersonIdOrderByIdAsc(personId).stream().sorted(Statement.ORDER).toList();
    }

    /** Moves one person's statements to another (on the same sheet or another), placed before the other's own in the
     *  order they stood, so the other's last statement stays theirs (or, having none, becomes the last moved). The
     *  moved statements are new; the old ones are deleted. */
    default void moveAll(Long fromPersonId, String toSheetId, Long toPersonId) {
        List<Statement> theirs = ofPerson(fromPersonId);
        if (theirs.isEmpty()) {
            return;
        }
        List<Statement> own = ofPerson(toPersonId);
        long first = own.isEmpty() ? 0 : own.get(0).getPlace();
        List<Statement> moved = new ArrayList<>();
        for (int i = 0; i < theirs.size(); i++) {
            moved.add(theirs.get(i).moveTo(toSheetId, toPersonId, first - theirs.size() + i));
        }
        deleteAll(theirs);
        flush();
        saveAllAndFlush(moved);
    }
}
