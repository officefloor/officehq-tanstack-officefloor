import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { useClaims } from '../claims/claims';
import { useSettings } from '../roles/roles';
import { useRates } from '../rates/rates';
import { personDue } from '../rates/due.slot';
import { gradeOn, usePeople } from './people';

// The export button: downloads every person on the sheet (retired people included, in list order) as CSV —
// name, grade in force on the sheet's today, retired-from date (blank when not retired) and due-total as shown.
function csvValue(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

function ExportPeople() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const today = useSheetToday();
  const settings = useSettings();
  if (!user || !sheet) return null;
  const download = () => {
    const lines = [['name', 'grade', 'retired-from', 'due-total']];
    for (const p of people.data ?? []) {
      lines.push([p.name, gradeOn(p, today), p.retiredFrom ?? '',
        personDue(p, claims.data ?? [], rates.data, today, settings.data)]);
    }
    const csv = lines.map((l) => l.map(csvValue).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `people-${sheet}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };
  return (
    <button type="button" data-testid="export-people" onClick={download}>
      Export people (CSV)
    </button>
  );
}

export const contribution = HomeMain.fill({ order: 19, Component: ExportPeople });
