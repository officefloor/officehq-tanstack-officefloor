import { useIsFetching } from '@tanstack/react-query';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePeople } from './people';
import { useSheetToday } from '../sheet/sheet';
import { showDue, useRates } from '../rates/rates';
import { useCapCents, useFloorCents } from '../settings/settings';
import { personDueCents, useClaims, useLate } from '../claims/claims';

// A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote or a line break.
const csvValue = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

// The button that downloads the people as CSV: everyone in list order, retired people included, with the grade in
// force on the sheet's today, the retired-from date (or nothing) and the due-total as their row shows it.
function ExportPeople({ sheet }: { sheet: string; user: string }) {
  const { data: people = [] } = usePeople(sheet);
  const { data: claims = [] } = useClaims(sheet);
  const { data: rates = [] } = useRates(sheet);
  const today = useSheetToday(sheet);
  const floor = useFloorCents(sheet);
  const cap = useCapCents(sheet);
  const late = useLate(sheet);

  // While the screen's reads are in flight (as after the as-of date changes) the figures are not yet the ones
  // shown, so the export waits for them.
  const reading = useIsFetching() > 0;

  const download = () => {
    const lines = [
      'name,grade,retired-from,due-total',
      ...people.map((p) =>
        [p.name, p.grade, p.retiredFrom ?? '', showDue(personDueCents(claims, rates, p, today, floor, late, cap, people))]
          .map(csvValue)
          .join(','),
      ),
    ];
    const url = URL.createObjectURL(new Blob([lines.map((l) => `${l}\n`).join('')], { type: 'text/csv' }));
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
      <button type="button" data-testid="export-people" disabled={reading} onClick={download}>
        Export people (CSV)
      </button>
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: 11, Component: ExportPeople });
