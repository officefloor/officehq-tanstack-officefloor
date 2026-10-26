import { useSearchParam } from '../../url/useSearchParam';

const asIds = (raw: unknown): number[] =>
  (Array.isArray(raw) ? raw : []).map(Number).filter((n) => Number.isInteger(n));

/** The ids of the people ticked for a bulk action, kept in the URL. */
export function useSelectedPeople(): [number[], (ids: number[]) => void] {
  const [ids, setIds] = useSearchParam('selectedPeople', asIds);
  return [ids, (next) => setIds(next.length ? next : undefined)];
}
