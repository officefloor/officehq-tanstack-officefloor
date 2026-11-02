import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { peopleKey, usePeople, type Person } from '../people/usePeople';

/** A person's hold: whether they are on hold, and a button that puts them on hold or takes them off
 *  it again. While held, their open (OPEN or PENDING) claims count in no live total; their SETTLED claims still do. */
function PersonHold({ sheetId, personId }: { sheetId: string; personId: number }) {
  const people = usePeople(sheetId);
  const queryClient = useQueryClient();
  const p = people.data?.find((x) => x.id === personId);

  const toggle = useMutation({
    mutationFn: () => postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/hold`, {}),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });

  return (
    <>
      <td data-testid="person-hold">{p?.held ? 'yes' : 'no'}</td>
      <td>
        <button
          type="button"
          data-testid="person-hold-toggle"
          disabled={toggle.isPending}
          onClick={() => {
            toggle.reset();
            toggle.mutate();
          }}
        >
          {p?.held ? 'Release' : 'Hold'}
        </button>
        {toggle.isError && <span data-testid="person-error">{toggle.error.message}</span>}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHead.fill({
    order: 45,
    Component: () => (
      <>
        <th>Held</th>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 45, Component: PersonHold }),
];
