import { PersonRow } from '../../slots/defs/personRow';
import { useClaims } from '../claims/claims';
import type { Person } from '../people/people';

/** A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote or a line
 *  break, else as it is (as the claims export writes it). */
function csvValue(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

/** The button that downloads a person's statement as a CSV file: a header line, then one line per claim of theirs
 *  their last statement bound, oldest first, with its number (or nothing), date, amount, the figure the statement
 *  bound it to as the claim shows it (n/a when it bound none) and status; each line ends with a line feed. */
function PersonExportStatement({ sheetId, person }: { sheetId: string; user: string; person: Person }) {
  const claims = useClaims(sheetId).data ?? [];

  const download = () => {
    const lines = [
      ['number', 'date', 'amount', 'figure', 'status'],
      // The claims come newest first: they are reversed to oldest first.
      ...claims.filter((c) => c.personId === person.id && c.stated).reverse().map((c) => [
        c.number ?? '',
        c.date,
        c.amount,
        c.statedDue ?? 'n/a',
        c.status,
      ]),
    ];
    const csv = lines.map((values) => values.map(csvValue).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `statement-${sheetId}-${person.id}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 0);
  };

  return (
    <td>
      <button type="button" data-testid="person-export-statement" onClick={download}>
        Export statement (CSV)
      </button>
    </td>
  );
}

export const contribution = PersonRow.fill({ order: 30.1, Component: PersonExportStatement });
