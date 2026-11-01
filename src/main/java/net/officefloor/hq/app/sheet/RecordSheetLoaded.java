package net.officefloor.hq.app.sheet;

import org.springframework.web.bind.annotation.PathVariable;

/** Records that the sheet has been loaded, so it exists. */
public class RecordSheetLoaded {
    public void record(@PathVariable("sheet") String sheet, SheetRegistry registry) {
        registry.loaded(sheet);
    }
}
