import { useSearchParam } from '../../url/useSearchParam';

const asIds = (raw: unknown): number[] =>
  Array.isArray(raw) ? raw.map(Number).filter((n) => Number.isInteger(n)) : [];

/** The claims ticked for a bulk action, by id. Kept in the URL, like any selection. */
export function useSelectedClaims(): [number[], (ids: number[]) => void] {
  const [ids, set] = useSearchParam('selected', asIds);
  return [ids, (next) => set(next.length === 0 ? undefined : next)];
}
