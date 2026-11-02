import { useIsFetching } from '@tanstack/react-query';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { gradeOn } from '../people/people';
import type { Person } from '../people/people';
import { claimRateOf, showDue, useRates } from '../rates/rates';
import { useFloorCents } from '../settings/settings';
import { bound, claimDueCents, useClaims, useLate } from '../claims/claims';

// A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote or a line break.
const csvValue = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

function ExportStatementHeader() {
  return <th />;
}

// The button that downloads the person's statement as CSV: a line per claim of theirs bound by their last statement,
// oldest first (by date, then as added), with its number, date, amount, the bound figure as its row shows it, and
// its status.
function PersonExportStatement({ sheet, person }: { sheet: string; user: string; person: Person }) {
  const { data: claims = [] } = useClaims(sheet);
  const { data: rates = [] } = useRates(sheet);
  const floor = useFloorCents(sheet);
  const late = useLate(sheet);

  // While the screen's reads are in flight the figures are not yet the ones shown, so the export waits for them.
  const reading = useIsFetching() > 0;

  const download = () => {
    const mine = claims
      .filter((c) => c.person === person.id && bound(c))
      .sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : a.id - b.id));
    const lines = [
      'number,date,amount,figure,status',
      ...mine.map((c) =>
        [
          String(c.periodNumber ?? c.number ?? ''),
          c.date,
          c.amount,
          showDue(claimDueCents(c, claimRateOf(rates, gradeOn(person, c.date), c), floor, late)),
          c.status,
        ]
          .map(csvValue)
          .join(','),
      ),
    ];
    const url = URL.createObjectURL(new Blob([lines.map((l) => `${l}\n`).join('')], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `statement-${sheet}-${person.id}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  return (
    <td>
      <button type="button" data-testid="person-export-statement" disabled={reading} onClick={download}>
        Export statement (CSV)
      </button>
    </td>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 21, Component: ExportStatementHeader }),
  PersonColumns.fill({ order: 21, Component: PersonExportStatement }),
];
