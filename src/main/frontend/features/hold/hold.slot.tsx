import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonHeads } from '../../slots/defs/personRow';
import { peopleKey, usePeople, type Person } from '../people/people.slot';
import { personActionKey, useLastPersonAction } from '../people/personAction';
import { useUser } from '../session/useSession';

function HoldHeads() {
  return (
    <>
      <th>On hold</th>
      <th />
    </>
  );
}

// Whether the person is on hold, and the button that holds or releases them. While held, their OPEN claims count
// in no live total; their SETTLED claims still do. A refusal shows on the person until the next action on their row.
function HoldCells({ sheetId, personId }: { sheetId: string; user: string; personId: number }) {
  const person = usePeople(sheetId).data?.find((p) => p.id === personId);
  const [user] = useUser();
  const queryClient = useQueryClient();
  const hold = useMutation({
    mutationKey: personActionKey(sheetId, personId),
    mutationFn: (held: boolean) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/hold`, {
        held,
        by: user ?? '',
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });
  const last = useLastPersonAction(sheetId, personId, hold.submittedAt);
  const held = person?.held ?? false;

  return (
    <>
      <td data-testid="person-hold">{held ? 'yes' : 'no'}</td>
      <td>
        <button type="button" data-testid="person-hold-toggle" disabled={hold.isPending} onClick={() => hold.mutate(!held)}>
          {held ? 'Release' : 'Hold'}
        </button>
        {hold.isError && last && (
          <span data-testid="person-error" role="alert">
            {hold.error.message}
          </span>
        )}
      </td>
    </>
  );
}

export const contributions = [
  PersonHeads.fill({ order: 45, Component: HoldHeads }),
  PersonCells.fill({ order: 45, Component: HoldCells }),
];
