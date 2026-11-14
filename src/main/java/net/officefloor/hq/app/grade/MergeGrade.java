package net.officefloor.hq.app.grade;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.CheckPersonNameUnique;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.NameTree;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Gives every person whose grade in force on the sheet's today is the merged grade the other one from that today, one
 * after another in list order, each exactly as the person form saves a grade change: PERSON_EDITED, then the rollup
 * lines it moved. One the person form would refuse (a name in use across the tree) is left as it is.
 */
public class MergeGrade {
    public void merge(@Val GradeMerge merge, @RequestParam("user") String user, SheetRepository sheets,
            PersonRepository people, RollupService rollups, NameTree tree, Audit audit, Out<GradeMerged> merged) {
        String sheetId = merge.sheetId();
        LocalDate today = sheets.today(sheetId);
        int count = 0;
        for (Person person : people.findAll(sheetId)) {
            if (!merge.from().equals(person.gradeOn(today))) {
                continue;
            }
            Rollup before = rollups.compute(sheetId);
            Person updated = person.withNameAndGrade(person.name(), merge.into(), today, today);
            try {
                new CheckPersonNameUnique().check(updated, people, tree);
            } catch (InvalidPersonException refused) {
                continue;
            }
            Person saved = people.save(updated);
            if (saved == null) {
                continue;
            }
            audit.record("PERSON_EDITED sheet=" + sheetId + " by=" + user + " name=" + saved.name() + " grade="
                    + saved.grade());
            RollupLines.record(before, rollups.compute(sheetId), sheetId, user, audit);
            count++;
        }
        merged.set(new GradeMerged(count));
    }
}
