import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { useSheetToday } from '../sheet/sheet';
import { useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { useRates } from '../rates/rates';
import { floorOf, useSettings } from '../settings/settings';
import { lateOf } from '../overdue/overdue';
import { personPeriodCarried, personPeriodDue, usePeriods } from './periods';
import { useFigure } from '../locale/locale';
import { openingOf, useOpenings } from '../openings/openings';

/** A CSV value: wrapped in double quotes, inner quotes doubled, when it holds a comma, quote or line break. */
const cell = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

// Downloads the period figures as CSV: person, period, due-total figure and carried figure as the person's
// row shows them, one line per person and period, people in list order and periods in the order added.
function ExportPeriods() {
  const fig = useFigure();
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const periods = usePeriods(sheet);
  const people = usePeople(sheet);
  const claims = useClaims(sheet);
  const rates = useRates(sheet);
  const openings = useOpenings(sheet);
  const today = useSheetToday(sheet);
  const settingsQuery = useSettings(sheet);
  const settings = settingsQuery.data;

  if (!user || !sheet) return null;

  // The figures are read as the rows show them: wait until every one is loaded.
  const ready = !!periods.data && !!people.data && !!claims.data && !!rates.data && !!openings.data
    && !!settingsQuery.data && today !== undefined;

  const download = () => {
    const floor = floorOf(settings);
    const added = [...(periods.data ?? [])].sort((a, b) => a.added - b.added);
    const lines = [['person', 'period', 'figure', 'carried']];
    for (const person of people.data ?? []) {
      const late = lateOf(settings, today, [person]);
      for (const p of added) {
        lines.push([
          person.name,
          p.name,
          fig(personPeriodDue(p, person, claims.data, rates.data, floor, late)),
          fig(personPeriodCarried(p, periods.data, person, claims.data, rates.data, floor, late,
            openingOf(openings.data, p.name, person.id))),
        ]);
      }
    }
    const csv = lines.map((l) => l.map(cell).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `periods-${sheet}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  return (
    <p>
      <button type="button" data-testid="export-periods" disabled={!ready} onClick={download}>
        Export period figures (CSV)
      </button>
    </p>
  );
}

export const contribution = HomeMain.fill({ order: 45.5, Component: ExportPeriods });
