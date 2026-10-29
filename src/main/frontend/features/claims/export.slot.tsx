import { useQuery } from '@tanstack/react-query';
import { downloadCsv } from '../../api/csv';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { peopleQuery } from '../people/peopleQuery';
import { periodOf, periodsQuery } from '../periods/periodsQuery';
import { figure, ratesQuery } from '../rates/ratesQuery';
import { useFloor, useLate } from '../settings/settingsQuery';
import { claimsQuery } from './claimsQuery';

/**
 * Downloads the claims as CSV: people in list order (retired people included, shown or not), each person's claims
 * oldest first, with the figures the screen shows (an empty number or period, and n/a, as they are).
 */
function ExportClaims({ sheetId }: { sheetId: string }) {
  const people = useQuery(peopleQuery(sheetId)).data ?? [];
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const rates = useQuery(ratesQuery(sheetId)).data ?? [];
  const periods = useQuery(periodsQuery(sheetId)).data ?? [];
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);

  const exportCsv = () => {
    downloadCsv('claims.csv', [
      ['person', 'number', 'date', 'amount', 'due', 'status', 'period'],
      ...people.flatMap((p) =>
        claims
          .filter((c) => c.personId === p.id)
          .reverse()
          .map((c) => [
            p.name,
            c.number ?? '',
            c.date,
            c.amount,
            figure(c, rates, p, floor, late),
            c.status,
            periodOf(periods, c.dueBy)?.name ?? '',
          ]),
      ),
    ]);
  };

  return (
    <p>
      <button type="button" data-testid="export-claims" onClick={exportCsv}>
        Export claims (CSV)
      </button>
    </p>
  );
}

export const contribution = SheetPanel.fill({ order: 11.5, Component: ExportClaims });
