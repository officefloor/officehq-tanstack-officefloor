import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';

// The generate button: for every plan on the sheet, in the order they were added, makes a DRAFT claim for each plan
// date up to its end and the sheet's today that has none yet, by the claim form's rules (a refused date is skipped
// and tried again next time). How many were made shows once done, and is gone while it runs.
function GeneratePlans() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const queryClient = useQueryClient();
  const generate = useMutation({
    mutationFn: () =>
      postJson<{ made: number }>(`/api/sheets/${encodeURIComponent(sheet ?? '')}/generate-plans`, {}, user),
    // Awaited: the count shows once the new rows do.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['sheet', sheet ?? ''] }),
  });
  const submit = useSubmitOnce(generate);
  if (!user || !sheet) return null;

  return (
    <div>
      <h2>Plans</h2>
      <button type="button" data-testid="plan-generate" disabled={generate.isPending} onClick={() => submit(undefined)}>
        Generate planned claims
      </button>
      {generate.isSuccess && (
        <>
          {' '}Made <span data-testid="plan-generated-count">{generate.data.made}</span>
        </>
      )}
      {generate.isError && <span data-testid="plan-generate-error">No claims could be generated.</span>}
    </div>
  );
}

export const contribution = HomeMain.fill({ order: 16.5, Component: GeneratePlans });
