import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { peopleKey, type Person } from '../people/usePeople';

/**
 * A person's hold: whether they are held, and the button to put them on hold or take them off it.
 * While held, their OPEN claims count in no live total; their SETTLED claims still count.
 */
function PersonHold({ user, sheet, person }: { user: string; sheet: string; person: Person }) {
  const queryClient = useQueryClient();
  const hold = useMutation({
    mutationFn: () => postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${person.id}/hold`, { user }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
  });
  return (
    <>
      <td data-testid="person-hold">{person.held ? 'yes' : 'no'}</td>
      <td>
        <button type="button" data-testid="person-hold-toggle" disabled={hold.isPending} onClick={() => hold.mutate()}>
          {person.held ? 'Release' : 'Hold'}
        </button>
        {hold.isError && (
          <span data-testid="person-error" role="alert">
            {' '}
            Could not change this person's hold.
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumns.fill({
    order: 35,
    Component: () => (
      <>
        <th>Held</th>
        <th />
      </>
    ),
  }),
  PersonCells.fill({ order: 35, Component: PersonHold }),
];
