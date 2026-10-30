import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { peopleKey, type Person } from './usePeople';

// Puts the person on hold, or takes them off hold when already held. While held their OPEN claims
// count in no live total; their SETTLED claims still do. A refusal shows the person's error.
export function HoldControl({ personId, held }: { personId: string; held: boolean }) {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const queryClient = useQueryClient();
  const once = useSubmitOnce();

  const hold = useMutation({
    mutationFn: () =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(personId)}/hold`, {
        user,
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
  });

  return (
    <>
      <button
        type="button"
        data-testid="person-hold-toggle"
        onClick={() => {
          hold.reset();
          once(() => hold.mutateAsync());
        }}
      >
        {held ? 'Release' : 'Hold'}
      </button>
      {hold.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          The person could not be put on or taken off hold.
        </span>
      )}
    </>
  );
}
