import { useSearchParam } from '../../url/useSearchParam';

/** The selected claims' ids, kept in the URL (`selected`, comma-separated) so a bulk action reads them. */
export function useSelectedClaims(): [number[], (ids: number[]) => void] {
  const [raw, setRaw] = useSearchParam('selected', (r) => (typeof r === 'string' || typeof r === 'number' ? String(r) : ''));
  const ids = raw
    .split(',')
    .filter((s) => s !== '')
    .map(Number)
    .filter((n) => !Number.isNaN(n));
  return [ids, (next) => setRaw(next.length === 0 ? undefined : next.join(','))];
}
