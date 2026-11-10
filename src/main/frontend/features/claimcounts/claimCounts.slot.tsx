import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { useClaims } from '../claims/useClaims';
import { countWithStatus } from '../summary/statusCounts';

/** The statuses counted on each person's row. */
const COUNTED = ['OPEN', 'SETTLED'] as const;

/** How many of a person's claims are OPEN and how many SETTLED (by their status alone, as the summary counts them). */
function PersonClaimCounts({ sheetId, personId }: { sheetId: string; personId: number }) {
  const claims = useClaims(sheetId);
  const mine = (claims.data ?? []).filter((c) => c.personId === personId);
  return (
    <>
      {COUNTED.map((s) => (
        <td key={s} data-testid={`person-claims-${s}-count`}>
          {countWithStatus(mine, s)}
        </td>
      ))}
    </>
  );
}

export const contributions = [
  PersonColumnHead.fill({
    order: 15,
    Component: () => (
      <>
        {COUNTED.map((s) => (
          <th key={s}>{s === 'OPEN' ? 'Open' : 'Settled'}</th>
        ))}
      </>
    ),
  }),
  PersonCells.fill({ order: 15, Component: PersonClaimCounts }),
];
