import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey, claimsPath } from './claims';
import type { Claim } from './claims';
import { clearSelectedClaims, useSelectedClaims } from './selection';

type BulkOpened = { opened: number; refused: number; claims: Claim[] };

// Opens the selected claims together: the server opens each that can be, in list order (people by
// their list positions, newest claim first), and leaves the rest as they are. Shows how many of each.
function BulkOpen({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const selected = useSelectedClaims(sheet);
  const open = useMutation({
    mutationFn: (ids: number[]) => postJson<BulkOpened>(byUser(`${claimsPath(sheet)}/bulk-open`, user), { claims: ids }),
    onSuccess: (result) => {
      queryClient.setQueryData(claimsKey(sheet), result.claims);
      clearSelectedClaims(sheet);
    },
  });
  const act = useSubmitOnce(open);

  return (
    <p>
      Selected claims: {selected.size}{' '}
      <button type="button" data-testid="bulk-open" disabled={open.isPending} onClick={() => act([...selected])}>
        Open selected
      </button>
      {open.isSuccess && (
        <>
          {' '}Opened <strong data-testid="bulk-opened">{open.data.opened}</strong>, refused{' '}
          <strong data-testid="bulk-refused">{open.data.refused}</strong>
        </>
      )}
      {open.isError && (
        <span data-testid="bulk-open-error" role="alert">
          {' '}The bulk open failed.
        </span>
      )}
    </p>
  );
}

export const contribution = SheetPanels.fill({ order: 21, Component: BulkOpen });
