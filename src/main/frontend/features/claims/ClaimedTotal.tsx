import { useQuery } from '@tanstack/react-query';
import { listPeople, peopleKey } from '../people/api';
import { claimsKey, correctsAnother, listClaims, liveClaims, sumAmounts } from './api';
import { Fig } from '../session/Fig';

/** The sheet's claimed-total: everyone's OPEN and SETTLED claims added up (but the OPEN claims dated
 *  within a hold of their person, unless a statement bound them), leaving out claims that correct another. */
export function ClaimedTotal({ sheetId }: { sheetId: string; user: string }) {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  return (
    <p>
      Claimed total <span data-testid="rollup-claimed-total"><Fig>{sumAmounts(liveClaims(claims.data ?? [], people.data).filter((c) => !correctsAnother(c)))}</Fig></span>
    </p>
  );
}
