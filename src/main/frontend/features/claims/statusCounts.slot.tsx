import { useQuery } from '@tanstack/react-query';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { claimsQuery } from './claimsQuery';
import type { ClaimStatus } from './claimsQuery';

/** The statuses whose claims are counted per person, each in its own column. */
const COUNTED: ClaimStatus[] = ['OPEN', 'SETTLED'];

function PersonStatusCount({ sheetId, personId, status }: { sheetId: string; personId: number; status: ClaimStatus }) {
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  return <td data-testid={`person-claims-${status}-count`}>{claims.filter((c) => c.personId === personId && c.status === status).length}</td>;
}

export const contributions = COUNTED.flatMap((status, i) => [
  PersonHeaders.fill({ order: 11 + i, Component: () => <th>{status === 'OPEN' ? 'Open' : 'Settled'}</th> }),
  PersonCells.fill({ order: 11 + i, Component: (ctx) => <PersonStatusCount {...ctx} status={status} /> }),
]);
