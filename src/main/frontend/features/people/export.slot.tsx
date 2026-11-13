import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { live, useClaims } from '../claims/claims';
import { lateOf } from '../overdue/overdue';
import { personDue, useRates } from '../rates/rates';
import { floorOf, useSettings } from '../settings/settings';
import { useSheetToday } from '../sheet/sheet';
import { gradeOn, usePeople } from './people';
import { useCapOf, useShared } from '../rates/due.slot';

/** A CSV value: wrapped in double quotes, inner quotes doubled, when it holds a comma, quote or line break. */
const cell = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

// Downloads the sheet's people as CSV: name, grade in force today, retired-from (or nothing) and
// due-total as the list shows it, one line per person in list order, retired people included.
function ExportPeople() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const today = useSheetToday(sheet);
  const settingsQuery = useSettings(sheet);
  const settings = settingsQuery.data;
  const shared = useShared(sheet);
  const capOf = useCapOf(sheet);

  if (!user || !sheet) return null;

  // The figures are read as of the screen's as-of date: wait until every one is loaded for it.
  const ready = !!people.data && !!claims.data && !!rates.data && !!settingsQuery.data && today !== undefined;

  const download = () => {
    const late = lateOf(settings, today, people.data);
    const lines = [['name', 'grade', 'retired-from', 'due-total']];
    for (const p of people.data ?? []) {
      const due = personDue(live((claims.data ?? []).filter((c) => c.personId === p.id), [p]), rates.data, p.grades, today, floorOf(settings), late, capOf(p), shared(p));
      lines.push([p.name, gradeOn(p.grades, today), p.retiredFrom ?? '', due]);
    }
    const csv = lines.map((l) => l.map(cell).join(',') + '\n').join('');
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
    <p>
      <button type="button" data-testid="export-people" disabled={!ready} onClick={download}>
        Export people (CSV)
      </button>
    </p>
  );
}

export const contribution = HomeMain.fill({ order: 21, Component: ExportPeople });
