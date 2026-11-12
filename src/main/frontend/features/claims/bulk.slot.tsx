import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HomeMain } from '../../slots/defs/homeMain';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { claimsKey, useSelectedClaims } from './claims';

type BulkOpenResult = { opened: number; refused: number };

// Opens the selected claims together: the server opens and numbers each that can be opened in list
// order and refuses the rest, and the counts show once it answers (gone while it runs).
function BulkOpen() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [selected, setSelected] = useSelectedClaims();
  const queryClient = useQueryClient();

  const bulk = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<BulkOpenResult>(`/api/sheets/${encodeURIComponent(sheet)}/bulk-open`, {
        claimIds: claimIds.map(String),
        user,
      }),
    onSuccess: async () => {
      setSelected([]);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheet) });
    },
  });
  const submit = useSubmitOnce(bulk);

  return (
    <p>
      <button type="button" data-testid="bulk-open" disabled={bulk.isPending} onClick={() => submit(selected)}>
        Open selected
      </button>
      {bulk.isSuccess && (
        <>
          {' '}
          Opened <strong data-testid="bulk-opened">{bulk.data.opened}</strong>, refused{' '}
          <strong data-testid="bulk-refused">{bulk.data.refused}</strong>
        </>
      )}
      {bulk.isError && (
        <span data-testid="bulk-open-error" role="alert">
          {' '}
          {bulk.error instanceof HttpError && bulk.error.reason ? `${bulk.error.reason}.` : 'The bulk open was refused.'}
        </span>
      )}
    </p>
  );
}

export const contribution = HomeMain.fill({ order: 22, Component: BulkOpen });
