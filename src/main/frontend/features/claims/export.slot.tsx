import { useIsFetching } from '@tanstack/react-query';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { gradeOn, usePeople } from '../people/people';
import { claimRateOf, showDue, useRates } from '../rates/rates';
import { periodOf, usePeriods } from '../periods/periods';
import { useFloorCents } from '../settings/settings';
import { claimDueCents, useClaims, useLate } from './claims';

// A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote or a line break.
const csvValue = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

// The button that downloads the claims as CSV: people in list order (retired people included), each person's claims
// oldest first, with the number, date, amount, due, status and period as the claim's row shows them.
function ExportClaims({ sheet }: { sheet: string; user: string }) {
  const { data: people = [] } = usePeople(sheet);
  const { data: claims = [] } = useClaims(sheet);
  const { data: rates = [] } = useRates(sheet);
  const { data: periods = [] } = usePeriods(sheet);
  const floor = useFloorCents(sheet);
  const late = useLate(sheet);

  // While the screen's reads are in flight (as after the as-of date changes) the figures are not yet the ones
  // shown, so the export waits for them.
  const reading = useIsFetching() > 0;

  const download = () => {
    const lines = [
      'person,number,date,amount,due,status,period',
      ...people.flatMap((p) =>
        // The claims are listed newest first; the export reads them oldest first.
        [...claims.filter((c) => c.person === p.id)].reverse().map((c) =>
          [
            p.name,
            String(c.periodNumber ?? c.number ?? ''),
            c.date,
            c.amount,
            showDue(claimDueCents(c, claimRateOf(rates, gradeOn(p, c.date), c), floor, late)),
            c.status,
            periodOf(periods, c.dueBy)?.name ?? '',
          ]
            .map(csvValue)
            .join(','),
        ),
      ),
    ];
    const url = URL.createObjectURL(new Blob([lines.map((l) => `${l}\n`).join('')], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `claims-${sheet}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  return (
    <p>
      <button type="button" data-testid="export-claims" disabled={reading} onClick={download}>
        Export claims (CSV)
      </button>
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: 12, Component: ExportClaims });
