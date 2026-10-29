import { useQueryClient } from '@tanstack/react-query';
import { downloadCsv } from '../../api/csv';
import { SheetPanel } from '../../slots/defs/sheetPanel';
import { poolsQuery } from './poolsQuery';

/** Downloads the pools as CSV: one line per member, pools and members in the order added, a pool member as pool:<name>
 *  and the cut as shown (empty when none). The pools are re-read first, so the file holds what the store holds now. */
function ExportPools({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();

  const exportCsv = async () => {
    const pools = await queryClient.fetchQuery({ ...poolsQuery(sheetId), staleTime: 0 });
    downloadCsv('pools.csv', [
      ['pool', 'member', 'share', 'cut'],
      ...pools.flatMap((p) => p.members.map((m) => [p.name, m.pool === null ? m.name : `pool:${m.pool}`, m.share, m.cut])),
    ]);
  };

  return (
    <p>
      <button type="button" data-testid="export-pools" onClick={() => void exportCsv()}>
        Export pools (CSV)
      </button>
    </p>
  );
}

export const contribution = SheetPanel.fill({ order: 6.81, Component: ExportPools });
