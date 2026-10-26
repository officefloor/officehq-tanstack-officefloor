import { useQuery } from '@tanstack/react-query';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery } from '../claims/claimsQuery';
import { personDue, ratesQuery } from '../rates/ratesQuery';
import { useFloor, useLate } from '../settings/settingsQuery';
import { useSheetToday } from '../sheet/useSheetToday';
import { gradeOn, peopleQuery } from './peopleQuery';

/** A CSV value: wrapped in double quotes, with inner quotes doubled, when it holds a comma, a quote or a line break. */
function csvValue(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

/** Downloads the people as CSV, in list order and retired people included, with the figures the screen shows. */
function ExportPeople({ sheetId }: { sheetId: string }) {
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const rates = useQuery(ratesQuery(sheetId)).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);

  const exportCsv = () => {
    const lines = [
      'name,grade,retired-from,due-total',
      ...people.map((p) =>
        [p.name, gradeOn(p, today) ?? p.grade, p.retiredFrom ?? '', personDue(p, people, claims, rates, today, floor, late)]
          .map(csvValue)
          .join(','),
      ),
    ];
    const url = URL.createObjectURL(new Blob([lines.map((l) => l + '\n').join('')], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = 'people.csv';
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
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
