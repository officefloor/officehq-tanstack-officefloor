import { useIsFetching, useQueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { poolsKey, poolsPath, type Pool } from './pools';

// A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote or a line break.
const csvValue = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

// The button that downloads the pools as CSV: one line per member, pools in the order added and members in the
// order added, a pool member as pool:<name>, and the cut as its row shows it (empty when none). The pools are
// re-read first, so a change elsewhere (a retirement, a setting) is reflected, and the list shows the same.
function ExportPools({ sheet }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();

  // While the screen's reads are in flight (as after the as-of date changes) the figures are not yet the ones
  // shown, so the export waits for them.
  const reading = useIsFetching() > 0;

  const download = async () => {
    const pools = await queryClient.fetchQuery({
      queryKey: poolsKey(sheet),
      queryFn: () => getJson<Pool[]>(poolsPath(sheet)),
      staleTime: 0,
    });
    const lines = [
      'pool,member,share,cut',
      ...pools.flatMap((pool) =>
        pool.members.map((m) =>
          [pool.name, m.memberPool !== null ? `pool:${m.memberPool}` : m.name, m.share, m.cut].map(csvValue).join(','),
        ),
      ),
    ];
    const url = URL.createObjectURL(new Blob([lines.map((l) => `${l}\n`).join('')], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `pools-${sheet}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  };

  return (
    <p>
      <button type="button" data-testid="export-pools" disabled={reading} onClick={() => void download()}>
        Export pools (CSV)
      </button>
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: 9.9, Component: ExportPools });
