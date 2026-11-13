import { useNavigate, useSearch } from '@tanstack/react-router';

/** The ids of the claims ticked for a bulk action, kept in the URL as a comma list under `claims`. */
export function useSelectedClaims(): [Set<number>, (id: number, on: boolean) => void, () => void] {
  const search = useSearch({ strict: false }) as Record<string, unknown>;
  const navigate = useNavigate();
  const selected = parse(search.claims);

  // Each change is applied to the URL as it is then, so ticks in quick succession never clobber each other.
  const update = (change: (ids: Set<number>) => void) => {
    void navigate({
      to: '.',
      search: (prev: Record<string, unknown>) => {
        const ids = parse(prev.claims);
        change(ids);
        const next = { ...prev };
        if (ids.size === 0) delete next.claims;
        else next.claims = [...ids].join(',');
        return next;
      },
      replace: true,
    });
  };

  const set = (id: number, on: boolean) => update((ids) => (on ? ids.add(id) : ids.delete(id)));
  const clear = () => update((ids) => ids.clear());
  return [selected, set, clear];
}

function parse(raw: unknown): Set<number> {
  return new Set(
    String(raw ?? '')
      .split(',')
      .filter((s) => s !== '')
      .map(Number)
      .filter((n) => !Number.isNaN(n)),
  );
}
