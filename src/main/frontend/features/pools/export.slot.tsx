import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { usePools } from './pools';

/** A CSV value: wrapped in double quotes, inner quotes doubled, when it holds a comma, quote or line break. */
const cell = (v: string) => (/[",\r\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v);

// Downloads the sheet's pools as CSV: pool, member (a pool member as pool:<name>), share and cut as the
// pools list shows them (cut empty when none), one line per member, pools and members in the order added.
function ExportPools() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const pools = usePools(sheet);

  if (!user || !sheet) return null;

  const download = () => {
    const lines = [['pool', 'member', 'share', 'cut']];
    for (const p of pools.data ?? []) {
      for (const m of p.members) {
        lines.push([p.name, m.memberPoolId !== null ? `pool:${m.name}` : m.name, m.share, m.cut ?? '']);
      }
    }
    const csv = lines.map((l) => l.map(cell).join(',') + '\n').join('');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
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
      <button type="button" data-testid="export-pools" disabled={!pools.data} onClick={download}>
        Export pools (CSV)
      </button>
    </p>
  );
}

export const contribution = HomeMain.fill({ order: 47.8, Component: ExportPools });
