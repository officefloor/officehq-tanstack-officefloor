import { useSearchParam } from '../../url/useSearchParam';

const asIds = (raw: unknown): number[] =>
  (Array.isArray(raw) ? raw : []).map(Number).filter((n) => Number.isInteger(n));

/** The ids of the claims ticked for a bulk action, kept in the URL. */
export function useSelectedClaims(): [number[], (ids: number[]) => void] {
  const [ids, setIds] = useSearchParam('selectedClaims', asIds);
  return [ids, (next) => setIds(next.length ? next : undefined)];
}
