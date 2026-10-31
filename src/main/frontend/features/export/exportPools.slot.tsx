import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePools } from '../pools/usePools';
import { downloadCsv } from './csv';

/**
 * Downloads the pools as a CSV file: a header line, then one line per member, pools in the order
 * added and each pool's members in the order added, a member that is another pool as pool:<name>,
 * with the share and the cut as the pool row shows them (the cut empty when none). Quoted and ended
 * as the people export.
 */
function ExportPools({ sheet }: { user: string; sheet: string }) {
  const { refetch } = usePools(sheet);

  // Re-read first, so the file holds the cuts as they stand (and the pool rows show the same).
  const download = async () => {
    const { data: pools = [] } = await refetch();
    const lines = [['pool', 'member', 'share', 'cut']];
    for (const p of pools) {
      for (const m of p.members) {
        lines.push([p.name, m.pool !== null ? `pool:${m.pool}` : m.name, m.share, m.cut ?? '']);
      }
    }
    downloadCsv(lines, `pools-${sheet}.csv`);
  };

  return (
    <p>
      <button type="button" data-testid="export-pools" onClick={() => void download()}>
        Export pools (CSV)
      </button>
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: 12.5, Component: ExportPools });
