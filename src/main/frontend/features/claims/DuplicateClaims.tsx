import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';

// Shows the sheet's duplicate claims: for each person, date and amount with two or more claims (VOID and
// DELETED left out), one line <name> <date> <amount> x<count>; people in list order, then by date, then by
// amount. Each press re-reads the store; the old lines are gone until the new ones are in.
export function DuplicateClaims() {
  const [sheet] = useSearchParam('sheet', asString);
  const dups = useMutation({
    mutationFn: () => getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/duplicate-claims`),
  });

  return (
    <section data-testid="dups">
      <button
        type="button"
        data-testid="dups-show"
        onClick={() => {
          dups.reset();
          dups.mutate();
        }}
      >
        Show duplicate claims
      </button>
      {dups.isSuccess &&
        dups.data.map((line, i) => (
          <div key={i} data-testid="dup-line">
            {line}
          </div>
        ))}
      {dups.isError && (
        <span data-testid="dups-error" role="alert">
          {' '}
          The duplicate claims could not be shown.
        </span>
      )}
    </section>
  );
}
