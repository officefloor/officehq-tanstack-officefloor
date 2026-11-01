import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { peopleKey, peoplePath } from './people';
import type { Person } from './people';

function HoldHeader() {
  return (
    <>
      <th>Hold</th>
      <th />
    </>
  );
}

// Whether the person is on hold, and the button that puts them on hold or takes them off it. While held,
// their OPEN claims count in no live total; their SETTLED ones still do.
function PersonHold({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const hold = useMutation({
    mutationFn: () => postJson<Person[]>(byUser(`${peoplePath(sheet)}/${person.id}/hold`, user), {}),
    onSuccess: (updated) => queryClient.setQueryData(peopleKey(sheet), updated),
  });
  const act = useSubmitOnce(hold);

  return (
    <>
      <td data-testid="person-hold">{person.held ? 'yes' : 'no'}</td>
      <td>
        <button type="button" data-testid="person-hold-toggle" disabled={hold.isPending} onClick={() => act(undefined)}>
          {person.held ? 'Release' : 'Hold'}
        </button>
        {hold.isError && (
          <span data-testid="person-error" role="alert">
            {' '}That hold was refused.
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 40, Component: HoldHeader }),
  PersonColumns.fill({ order: 40, Component: PersonHold }),
];
