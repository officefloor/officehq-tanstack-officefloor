package net.officefloor.hq.app.sheet;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SheetRepository extends JpaRepository<Sheet, String> {

    /** The ids of the sheet's children: the sheets naming it as their parent (their undated parent setting), by id. */
    @Query("select distinct s.sheet.id from SheetSetting s where s.key = 'parent' and s.value = :sheetId order by s.sheet.id")
    List<String> childrenOf(@Param("sheetId") String sheetId);

    /** The parent sheet a sheet names (its undated parent setting); empty (or a null value) when none. */
    @Query("select s.value from SheetSetting s where s.sheet.id = :sheetId and s.key = 'parent'")
    List<String> parentsOf(@Param("sheetId") String sheetId);

    /** The ids of the sheet's descendants: its children, their children, and so on. */
    default Set<String> descendantsOf(String sheetId) {
        Set<String> seen = new HashSet<>();
        Deque<String> todo = new ArrayDeque<>();
        todo.push(sheetId);
        while (!todo.isEmpty()) {
            for (String child : childrenOf(todo.pop())) {
                if (!child.equals(sheetId) && seen.add(child)) {
                    todo.push(child);
                }
            }
        }
        return seen;
    }
}
