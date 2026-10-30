import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { usePersonAction } from './usePersonAction';
import { peopleKey, type Person } from './usePeople';

// Moves the person up or down the list, swapping them with their neighbour that way. The top person
// cannot move up nor the bottom person down: a refusal shows the person's error.
export function MoveControl({ personId }: { personId: string }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const move = useMutation({
    mutationFn: (up: boolean) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/move`, {
        up,
        user,
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
  });
  const taken = usePersonAction(personId, () => move.reset());

  const button = (up: boolean) => (
    <button
      type="button"
      data-testid={up ? 'person-move-up' : 'person-move-down'}
      onClick={() => {
        taken();
        once(() => move.mutateAsync(up));
      }}
    >
      {up ? 'Up' : 'Down'}
    </button>
  );
  return (
    <>
      {button(true)} {button(false)}
      {move.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          The person could not be moved.
        </span>
      )}
    </>
  );
}
