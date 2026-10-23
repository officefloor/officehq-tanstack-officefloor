import { useQueryClient } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheet';
import { poolsKey } from './pools';
import type { Pool } from './pools';

/** A CSV value: wrapped in double quotes (any quote inside doubled) when it holds a comma, a quote or a line
 *  break, else as it is. */
function csvValue(value: string): string {
  return /[",\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

/** The button that downloads the pools as a CSV file: a header line, then one line per member, pools in the order
 *  added and members in the order added, a member pool as pool:<name>, and the cut as shown on the pool (empty when
 *  none); each line ends with a line feed. The pools are re-read first, so the file matches the store. */
function ExportPools({ sheetId }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();

  const download = async () => {
    const pools = await queryClient.fetchQuery({
      queryKey: poolsKey(sheetId),
      queryFn: () => getJson<Pool[]>(`/api/sheets/${encodeURIComponent(sheetId)}/pools`),
      staleTime: 0,
    });
    const lines = [
      ['pool', 'member', 'share', 'cut'],
      ...pools.flatMap((p) =>
        p.members.map((m) => [p.name, m.poolId !== null ? `pool:${m.name}` : m.name, m.share, m.cut]),
      ),
    ];
    const csv = lines.map((values) => values.map(csvValue).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `pools-${sheetId}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 0);
  };

  return (
    <p>
      <button type="button" data-testid="export-pools" onClick={() => void download()}>
        Export pools (CSV)
      </button>
    </p>
  );
}

export const contribution = SheetBody.fill({ order: 7.9, Component: ExportPools });
