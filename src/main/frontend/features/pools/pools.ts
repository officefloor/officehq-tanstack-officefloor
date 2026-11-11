import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { peopleKey } from '../people/people';

/** A person in a pool, or another pool (memberPool its name, personId ''), with a share percent (two decimals). A
 *  retired member's share counts toward the total. Their cut is their part of the pool's pot, two decimals
 *  ('' when the pool is unbalanced, there is no pot, or every member is retired); a retired person, or one with a
 *  hold in force on the sheet's today, gets 0.00, their share spread over the members with a cut. A pool that is a member of no pool has the sheet's pot; one
 *  that is a member of another has its cut there as its pot. */
export type PoolMember = {
  personId: string;
  memberPool: string;
  name: string;
  share: string;
  retired: boolean;
  cut: string;
  /** The membership's from- and to-dates ('' for open). */
  from: string;
  to: string;
  /** Whether the dates include the sheet's today: a member not active is left out of the total, the balance and
   *  the cuts (its cut is ''). */
  active: boolean;
};

/** A pool on the sheet: its members in the order added, their total share, and balanced when it is exactly 100.00.
 *  `added` orders the pools as they were added (a larger one added later; a crew's after every pool). `crew` is
 *  whether it is a crew's pool: its lead and members at equal shares (100.00 split as patrons' shares are, the
 *  remainder on the lead), kept by the crew, so no member is added to it here and it is no other pool's member. */
export type Pool = {
  name: string;
  members: PoolMember[];
  totalShare: string;
  balanced: boolean;
  added: number;
  crew: boolean;
};

// Under the people's key: a change to a person (a retirement, a new name) reads the pools again too.
export const poolsKey = (sheetId: string) => [...peopleKey(sheetId), 'pools'] as const;

/** The pools on the sheet, by name. */
export function usePools(sheetId: string) {
  return useQuery({
    queryKey: poolsKey(sheetId),
    queryFn: async () =>
      (await getJson<{ pools: Pool[] }>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`)).pools,
  });
}
