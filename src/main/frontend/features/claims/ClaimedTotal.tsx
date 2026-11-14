import { useQuery } from '@tanstack/react-query';
import { claimsKey, listClaims, sumAmounts } from './api';

/** The sheet's claimed-total: everyone's claims added up. */
export function ClaimedTotal({ sheetId }: { sheetId: string; user: string }) {
  const claims = useQuery({ queryKey: claimsKey(sheetId), queryFn: () => listClaims(sheetId) });
  return (
    <p>
      Claimed total <span data-testid="rollup-claimed-total">{sumAmounts(claims.data ?? [])}</span>
    </p>
  );
}
