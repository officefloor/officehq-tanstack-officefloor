import { useSearchParam } from '../../url/useSearchParam';

/** The selected people's ids, kept in the URL (`selectedPeople`, comma-separated) so a bulk action on people reads them. */
export function useSelectedPeople(): [number[], (ids: number[]) => void] {
  const [raw, setRaw] = useSearchParam('selectedPeople', (r) => (typeof r === 'string' || typeof r === 'number' ? String(r) : ''));
  const ids = raw
    .split(',')
    .filter((s) => s !== '')
    .map(Number)
    .filter((n) => !Number.isNaN(n));
  return [ids, (next) => setRaw(next.length === 0 ? undefined : next.join(','))];
}
