import { useQuery } from '@tanstack/react-query';
import { listPools, poolsKey, sheetCutTotal } from './api';
import { Fig } from '../session/Fig';

/** The sheet's cut total: everyone's cuts across the pools added up. */
export function CutTotal({ sheetId }: { sheetId: string; user: string }) {
  const pools = useQuery({ queryKey: poolsKey(sheetId), queryFn: () => listPools(sheetId) });
  return (
    <p>
      Cut total <span data-testid="rollup-cut-total"><Fig>{sheetCutTotal(pools.data ?? [])}</Fig></span>
    </p>
  );
}
