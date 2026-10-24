import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { peoplePath, peopleQueryKey, type Person } from './people';

// Whether a person is on hold, and the button that puts them on hold or takes them off it. While held, their
// OPEN claims count in no live total; their SETTLED claims still count.
function HoldHeader() {
  return <th>Hold</th>;
}

function PersonHold({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();
  const hold = useMutation({
    mutationFn: (body: { held: boolean }) => putJson<Person>(`${peoplePath(sheet ?? '')}/${person.id}/hold`, body, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') }),
  });
  const submit = useSubmitOnce(hold);
  return (
    <td>
      <span data-testid="person-hold">{person.held ? 'yes' : 'no'}</span>{' '}
      <button
        type="button"
        data-testid="person-hold-toggle"
        disabled={hold.isPending}
        onClick={() => submit({ held: !person.held })}
      >
        {person.held ? 'Release' : 'Hold'}
      </button>
      {hold.isError && <span data-testid="person-error">The hold could not be changed.</span>}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 45, Component: HoldHeader }),
  PersonCells.fill({ order: 45, Component: PersonHold }),
];
