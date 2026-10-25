import { useQueryClient } from '@tanstack/react-query';
import { figureToday } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { sheetQueryKey, useCurrentSheet, useCurrentUser, type SheetInfo } from '../session/session';
import { peoplePath, peopleQueryKey, type Person } from '../people/people';
import { csvValue, readFresh, sheetInfoPath } from '../people/export.slot';
import { ratesPath, ratesQueryKey, type Rate } from '../rates/rates';
import { settingsPath, settingsQueryKey, type Settings } from '../roles/roles';
import { claimsPath, claimsQueryKey, type Claim } from '../claims/claims';
import { personPeriodCarried, personPeriodDue, periodsPath, periodsQueryKey, type Period } from './periods';

// The period export button: downloads the period figures as CSV — person, period, figure (the person's due-total
// for it) and carried, as the person's row shows them — one line per person and period, people in list order
// (retired people included, shown or not) and periods in the order added. Quoting and line ends as the people
// export; its inputs are re-read from the store when pressed, as the people export's.
function ExportPeriods() {
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
    const today = figureToday(info.today);
    // The sheet's periods come by from-date; ids are assigned in the order they were added.
    const added = [...periods].sort((a, b) => a.id - b.id);
    const lines = [['person', 'period', 'figure', 'carried']];
    for (const p of people) {
      for (const q of added) {
        lines.push([p.name, q.name,
          personPeriodDue(q, p, people, claims, rates, settings, today),
          personPeriodCarried(q, periods, p, people, claims, rates, settings, today)]);
      }
    }
    const csv = lines.map((l) => l.map(csvValue).join(',') + '\n').join('');
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
    <button type="button" data-testid="export-periods" onClick={() => { void download(); }}>
      Export periods (CSV)
    </button>
  );
}

export const contribution = HomeMain.fill({ order: 19.05, Component: ExportPeriods });
