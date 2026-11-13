import { useNavigate, useSearch } from '@tanstack/react-router';

/** The ids of the people ticked for a bulk action, kept in the URL as a comma list under `people`. */
export function useSelectedPeople(): [Set<number>, (id: number, on: boolean) => void, () => void] {
  const search = useSearch({ strict: false }) as Record<string, unknown>;
  const navigate = useNavigate();
  const selected = parse(search.people);

  // Each change is applied to the URL as it is then, so ticks in quick succession never clobber each other.
  const update = (change: (ids: Set<number>) => void) => {
    void navigate({
      to: '.',
      search: (prev: Record<string, unknown>) => {
        const ids = parse(prev.people);
        change(ids);
        const next = { ...prev };
        if (ids.size === 0) delete next.people;
        else next.people = [...ids].join(',');
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
