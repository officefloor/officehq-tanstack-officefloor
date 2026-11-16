import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { peopleKey, type Person } from '../people/people';

function HoldHead() {
  return (
    <>
      <th>On hold</th>
      <th />
    </>
  );
}

/** Whether the person is on hold, and the button that holds or releases them. While held, their OPEN or PENDING claims count in
 *  no live total; their SETTLED claims still do. A refused hold or release shows the row's error. */
function PersonHold({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const submit = useSubmitOnce();
  const hold = useMutation({
    mutationFn: (held: boolean) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/hold`, { held, by: user }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
    onSettled: submit.end,
  });

  return (
    <>
      <td data-testid="person-hold">{person.held ? 'yes' : 'no'}</td>
      <td>
        <button
          type="button"
          data-testid="person-hold-toggle"
          disabled={hold.isPending}
          onClick={() => {
            if (!submit.begin()) return;
            hold.reset();
            hold.mutate(!person.held);
          }}
        >
          {person.held ? 'Release' : 'Hold'}
        </button>
        {hold.isError && <span data-testid="person-error"> Could not change this person's hold.</span>}
      </td>
    </>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 50, Component: HoldHead }),
  PersonRow.fill({ order: 50, Component: PersonHold }),
];
