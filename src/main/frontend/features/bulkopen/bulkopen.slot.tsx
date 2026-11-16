import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { SheetPage } from '../../slots/defs/sheetPage';
import { claimsKey } from '../claims/claims';
import { useSelectedClaims } from '../claims/selection';

type Result = { opened: number; refused: number };

/** Opens the selected claims together: each that can be opened is opened and numbered in list order (people in
 *  order, a person's claims newest first); each that cannot is left as it is. Shows how many were opened and how
 *  many refused; the counts are cleared while a new press runs. */
function BulkOpen({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useSelectedClaims();
  const open = useMutation({
    mutationFn: (claimIds: number[]) =>
      postJson<Result>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-open`, { claimIds, by: user }),
    onSuccess: async () => {
      setSelected([]);
      await queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) });
    },
  });

  return (
    <p>
      <button
        type="button"
        data-testid="bulk-open"
        disabled={open.isPending}
        onClick={() => {
          open.reset();
          open.mutate(selected);
        }}
      >
        Open selected ({selected.length})
      </button>
      {open.isSuccess && (
        <>
          {' '}
          Opened <span data-testid="bulk-opened">{open.data.opened}</span>, refused{' '}
          <span data-testid="bulk-refused">{open.data.refused}</span>
        </>
      )}
      {open.isError && <span data-testid="bulk-open-error"> Could not open the selected claims.</span>}
    </p>
  );
}

export const contribution = SheetPage.fill({ order: 25, Component: BulkOpen });
