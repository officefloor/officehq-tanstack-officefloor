package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingLogRepository extends JpaRepository<SettingLog, Long> {

    /** The sheet's settings set, in the order set. */
    List<SettingLog> findBySheetIdOrderByIdAsc(String sheetId);

    /** The sheet as it stood at the end of a date, that date standing in for its today: every setting set while
     *  its today was on or before it — but the parent sheet it names now, which is not dated. Only worked out, never
     *  saved. */
    default Sheet sheetAsOf(Sheet sheet, LocalDate date) {
        Sheet then = new Sheet(sheet.getId());
        then.setToday(date);
        then.setArchived(sheet.archivedBy(date));
        for (SettingLog set : findBySheetIdOrderByIdAsc(sheet.getId())) {
            if (set.getMadeOn().isAfter(date) || Sheet.PARENT.equals(set.getKey())) {
                continue;
            }
            if (Sheet.POLICY_DATE.equals(set.getKey())) {
                then.setPolicyDate(set.getValue() == null ? null : LocalDate.parse(set.getValue()));
            } else if (Sheet.PERIOD_NUMBERING_FROM.equals(set.getKey())) {
                then.setPeriodNumberingFrom(set.getValue() == null ? null : LocalDate.parse(set.getValue()));
            } else if (Sheet.ENTRY_DATE.equals(set.getKey())) {
                then.setEntryDate(set.getValue() == null ? null : LocalDate.parse(set.getValue()));
            } else {
                then.record(set.getKey(), set.getFromDate(), set.getValue());
            }
        }
        then.record(Sheet.PARENT, null, sheet.getParent());
        return then;
    }
}
