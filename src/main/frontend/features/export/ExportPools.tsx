import { useQueryClient } from '@tanstack/react-query';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { csvField } from './ExportPeople';
import { freshPools } from './fresh';

// The button that downloads the pools as a CSV file: one line per member, pools in the order added and
// members in the order added, a pool member as pool:<name> and the cut as the pool's row shows it (empty
// when none). The pools are re-read from the store first, so the cuts are current (as of the screen's
// as-of date, when one is set). Quoted and ended as the people export.
export function ExportPools() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();

  if (!user || !sheet) return null;

  const download = async () => {
    const pools = await freshPools(queryClient, sheet);
    const lines = [['pool', 'member', 'share', 'cut']];
    for (const p of pools) {
      for (const m of p.members) {
        lines.push([p.name, m.poolId !== null ? `pool:${m.name}` : m.name, m.share, m.cut]);
      }
    }
    const csv = lines.map((l) => l.map(csvField).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `pools-${sheet}.csv`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  return (
    <button type="button" data-testid="export-pools" onClick={download}>
      Export pools (CSV)
    </button>
  );
}
