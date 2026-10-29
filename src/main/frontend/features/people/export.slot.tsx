import { useQuery } from '@tanstack/react-query';
import { downloadCsv } from '../../api/csv';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery } from '../claims/claimsQuery';
import { useCapFor } from '../pools/poolsQuery';
import { personDue, ratesQuery } from '../rates/ratesQuery';
import { useFloor, useLate } from '../settings/settingsQuery';
import { useSheetToday } from '../sheet/useSheetToday';
import { gradeOn, peopleQuery } from './peopleQuery';

/** Downloads the people as CSV, in list order and retired people included, with the figures the screen shows. */
function ExportPeople({ sheetId }: { sheetId: string }) {
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const rates = useQuery(ratesQuery(sheetId)).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const capFor = useCapFor(sheetId);

  const exportCsv = () => {
    downloadCsv('people.csv', [
      ['name', 'grade', 'retired-from', 'due-total'],
      ...people.map((p) => [p.name, gradeOn(p, today) ?? p.grade, p.retiredFrom ?? '', personDue(p, people, claims, rates, today, floor, late, capFor(p.id))]),
    ]);
  };

  return (
    <p>
      <button type="button" data-testid="export-people" onClick={exportCsv}>
        Export people (CSV)
      </button>
    </p>
  );
}

export const contribution = SheetPanel.fill({ order: 11, Component: ExportPeople });
