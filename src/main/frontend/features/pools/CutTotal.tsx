import { useQuery } from '@tanstack/react-query';
import { listPools, personCutTotal, poolsKey } from './api';
import { listPeople, peopleKey } from '../people/api';
import { trimmedSum } from '../rates/api';
import { Fig } from '../session/Fig';

/** Each person's cut total as their row shows it, in list order. */
export function usePersonCuts(sheetId: string): string[] {
  const people = useQuery({ queryKey: peopleKey(sheetId), queryFn: () => listPeople(sheetId) });
  const pools = useQuery({ queryKey: poolsKey(sheetId), queryFn: () => listPools(sheetId) });
  return (people.data ?? []).map((p) => personCutTotal(pools.data ?? [], p.id));
}

/** The sheet's cut total: everyone's cut total added up, less the single highest and lowest when three or
 *  more have one (see `trimmedSum`). */
export function CutTotal({ sheetId }: { sheetId: string; user: string }) {
  return (
    <p>
      Cut total <span data-testid="rollup-cut-total"><Fig>{trimmedSum(usePersonCuts(sheetId))}</Fig></span>
    </p>
  );
}
