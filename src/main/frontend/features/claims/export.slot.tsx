import { useQueryClient } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { sheetQueryKey, useCurrentSheet, useCurrentUser, type SheetInfo } from '../session/session';
import { gradeOn, peoplePath, peopleQueryKey, type Person } from '../people/people';
import { csvValue, readFresh, sheetInfoPath } from '../people/export.slot';
import { chargedOf, ratesPath, ratesQueryKey, type Rate } from '../rates/rates';
import { periodOn, periodsPath, periodsQueryKey, type Period } from '../periods/periods';
import { settingsPath, settingsQueryKey, type Settings } from '../roles/roles';
import { claimsPath, claimsQueryKey, dueByOf, type Claim } from './claims';

// The claims export button: downloads every claim on the sheet as CSV — people in list order (retired people
// included, shown or not), each person's claims oldest first — with person, number, date, amount, due, status and
// period as the claim's row shows them. Quoting and line ends as the people export. Its inputs are re-read from the
// store when pressed, as the people export's, so in an as-of view it has the claims and figures as of that date.
function ExportClaims() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();
  if (!user || !sheet) return null;
  const download = async () => {
    const [info, people, claims, rates, periods, settings] = await Promise.all([
      readFresh<SheetInfo>(queryClient, sheetQueryKey(sheet), sheetInfoPath(sheet)),
      readFresh<Person[]>(queryClient, peopleQueryKey(sheet), peoplePath(sheet)),
      readFresh<Claim[]>(queryClient, claimsQueryKey(sheet), claimsPath(sheet)),
      readFresh<Rate[]>(queryClient, ratesQueryKey(sheet), ratesPath(sheet)),
      readFresh<Period[]>(queryClient, periodsQueryKey(sheet), periodsPath(sheet)),
      readFresh<Settings>(queryClient, settingsQueryKey(sheet), settingsPath(sheet)),
    ]);
    const today = info.today;
    const lines = [['person', 'number', 'date', 'amount', 'due', 'status', 'period']];
    for (const p of people) {
      // The sheet's claims come newest first.
      const mine = claims.filter((c) => c.personId === p.id).reverse();
      for (const c of mine) {
        lines.push([p.name, c.number ?? '', c.date, c.amount,
          chargedOf(c, rates, gradeOn(p, c.date), settings, today), c.status,
          periodOn(periods, dueByOf(c))?.name ?? '']);
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
    <button type="button" data-testid="export-claims" onClick={() => { void download(); }}>
      Export claims (CSV)
    </button>
  );
}

export const contribution = HomeMain.fill({ order: 19.1, Component: ExportClaims });
