package net.officefloor.hq.app.sheet;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SheetRepository extends JpaRepository<Sheet, String> {

    /** The ids of the sheet's children: the sheets naming it as their parent (their undated parent setting), by id. */
    @Query("select distinct s.sheet.id from SheetSetting s where s.key = 'parent' and s.value = :sheetId order by s.sheet.id")
    List<String> childrenOf(@Param("sheetId") String sheetId);
}
