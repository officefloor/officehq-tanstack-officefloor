import { useQueryClient } from '@tanstack/react-query';
import { downloadCsv } from '../../api/csv';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { claimsQuery } from '../claims/claimsQuery';
import { peopleQuery } from '../people/peopleQuery';
import { ratesQuery } from '../rates/ratesQuery';
import { useFloor, useLate } from '../settings/settingsQuery';
import { personPeriodCarried, personPeriodDue, periodsQuery } from './periodsQuery';

/** Downloads the period figures as CSV: one line per person and period, people in list order (retired people included)
 *  and periods in the order added, with each person's due-total and carried figure for the period as their row shows
 *  them. The data is re-read first, so the file holds what the store holds now. */
function ExportPeriods({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);

  const exportCsv = async () => {
    const fresh = { staleTime: 0 };
    const [periods, people, claims, rates] = await Promise.all([
      queryClient.fetchQuery({ ...periodsQuery(sheetId), ...fresh }),
      queryClient.fetchQuery({ ...peopleQuery(sheetId), ...fresh }),
      queryClient.fetchQuery({ ...claimsQuery(sheetId), ...fresh }),
      queryClient.fetchQuery({ ...ratesQuery(sheetId), ...fresh }),
    ]);
    const added = [...periods].sort((a, b) => a.added - b.added);
    downloadCsv('periods.csv', [
      ['person', 'period', 'figure', 'carried'],
      ...people.flatMap((person) =>
        added.map((p) => [
          person.name,
          p.name,
          personPeriodDue(p, person, claims, people, rates, floor, late),
          personPeriodCarried(p, periods, person, claims, people, rates, floor, late),
        ]),
      ),
    ]);
  };

  return (
    <p>
      <button type="button" data-testid="export-periods" onClick={() => void exportCsv()}>
        Export period figures (CSV)
      </button>
    </p>
  );
}

export const contribution = SheetPanel.fill({ order: 7.1, Component: ExportPeriods });
