import { useIsFetching } from '@tanstack/react-query';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople } from '../people/people';
import { useRates } from '../rates/rates';
import { useFloorCents } from '../settings/settings';
import { useClaims, useLate } from '../claims/claims';
import {
  personPeriodCarriedCents,
  personPeriodDueCents,
  shownPersonCarried,
  shownPersonDueTotal,
  usePeriods,
} from './periods';

// A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote or a line break.
const csvValue = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

// The button that downloads the period figures as CSV: one line per person (in list order) and period (in the order
// added), with the person's due-total and carried figure for the period as their row shows them.
function ExportPeriods({ sheet }: { sheet: string; user: string }) {
  const { data: periods = [] } = usePeriods(sheet);
  const { data: claims = [] } = useClaims(sheet);
  const { data: people = [] } = usePeople(sheet);
  const { data: rates = [] } = useRates(sheet);
  const floor = useFloorCents(sheet);
  const late = useLate(sheet);

  // While the screen's reads are in flight the figures are not yet the ones shown, so the export waits for them.
  const reading = useIsFetching() > 0;

  const download = () => {
    const totals = personPeriodDueCents(periods, claims, people, rates, floor, late);
    const carried = personPeriodCarriedCents(periods, claims, people, rates, floor, late);
    const lines = [
      'person,period,figure,carried',
      ...people.flatMap((person) =>
        [...periods].sort((a, b) => a.added - b.added).map((p) =>
          [
            person.name,
            p.name,
            shownPersonDueTotal(p, person.id, totals.get(p.name)?.get(person.id) ?? 0n),
            shownPersonCarried(p, person.id, carried.get(p.name)?.get(person.id) ?? 0n),
          ]
            .map(csvValue)
            .join(','),
        ),
      ),
    ];
    const url = URL.createObjectURL(new Blob([lines.map((l) => `${l}\n`).join('')], { type: 'text/csv' }));
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
      <button type="button" data-testid="export-periods" disabled={reading} onClick={download}>
        Export period figures (CSV)
      </button>
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: 11, Component: ExportPeriods });
