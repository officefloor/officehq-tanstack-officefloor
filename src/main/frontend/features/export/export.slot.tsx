import { SheetPanels } from '../../slots/defs/sheetPanels';
import { live, personDueCents, useClaims } from '../claims/useClaims';
import { gradeOn, heldOnBy, usePeople } from '../people/usePeople';
import { claimRateOn, rateOn, showDue, useRates } from '../rates/useRates';
import { useCutTotal } from '../pools/usePools';
import { useCap, useFloor, useLate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheet';
import { downloadCsv, useSheetRead } from './csv';

/**
 * Downloads the people as a CSV file: a header line, then one line per person in list order,
 * retired people included, with the grade in force on the sheet's today, the retired-from date
 * (empty when not retired) and the due-total as the people list shows it — while an as-of date is
 * set, all as of that date (the button waits for those figures). Lines end with a line feed.
 */
function ExportPeople({ sheet }: { user: string; sheet: string }) {
  const { data: people = [] } = usePeople(sheet);
  const { data: all = [] } = useClaims(sheet);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloor(sheet);
  const late = useLate(sheet);
  const cap = useCap(sheet);
  const cutOf = useCutTotal(sheet);

  const read = useSheetRead(sheet);

  const download = () => {
    const claims = live(all, heldOnBy(people, today));
    const lines = [['name', 'grade', 'retired-from', 'due-total']];
    for (const p of people) {
      const due = personDueCents(
        claims.filter((c) => c.personId === p.id),
        floor,
        rateOn(rates, gradeOn(p, today), today),
        (c) => claimRateOn(rates, gradeOn(p, c.date), c),
        late,
        cap,
        undefined,
        cutOf(p.id),
      );
      lines.push([p.name, gradeOn(p, today), p.retiredFrom ?? '', showDue(due)]);
    }
    downloadCsv(lines, `people-${sheet}.csv`);
  };

  return (
    <p>
      <button type="button" data-testid="export-people" disabled={!read} onClick={download}>
        Export people (CSV)
      </button>
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: 11, Component: ExportPeople });
