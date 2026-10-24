import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser, useSheetToday } from '../session/session';
import { gradeOn, usePeople } from '../people/people';
import { csvValue } from '../people/export.slot';
import { chargedOf, useRates } from '../rates/rates';
import { periodOn, usePeriods } from '../periods/periods';
import { useSettings } from '../roles/roles';
import { dueByOf, useClaims } from './claims';

// The claims export button: downloads every claim on the sheet as CSV — people in list order (retired people
// included, shown or not), each person's claims oldest first — with person, number, date, amount, due, status and
// period as the claim's row shows them. Quoting and line ends as the people export.
function ExportClaims() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople();
  const claims = useClaims();
  const rates = useRates();
  const periods = usePeriods();
  const settings = useSettings();
  const today = useSheetToday();
  if (!user || !sheet) return null;
  const download = () => {
    const lines = [['person', 'number', 'date', 'amount', 'due', 'status', 'period']];
    for (const p of people.data ?? []) {
      // The sheet's claims come newest first.
      const mine = (claims.data ?? []).filter((c) => c.personId === p.id).reverse();
      for (const c of mine) {
        lines.push([p.name, c.number ?? '', c.date, c.amount,
          chargedOf(c, rates.data, gradeOn(p, c.date), settings.data, today), c.status,
          periodOn(periods.data, dueByOf(c))?.name ?? '']);
      }
    }
    const csv = lines.map((l) => l.map(csvValue).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `claims-${sheet}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };
  return (
    <button type="button" data-testid="export-claims" onClick={download}>
      Export claims (CSV)
    </button>
  );
}

export const contribution = HomeMain.fill({ order: 19.1, Component: ExportClaims });
