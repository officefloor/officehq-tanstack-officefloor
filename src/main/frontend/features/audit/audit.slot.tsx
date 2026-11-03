import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';

/** The audit viewer: the sheet's audit lines exactly as the audit file holds them, newest first. A name
 *  in the filter keeps only the lines about that person (name=<name> or person=<name>, compared ignoring
 *  case and surrounding spaces), followed by the matching lines of the sheet's descendants (each sheet's
 *  newest first, sheets by id) and then those from the sheet a transferred person came from. It reads the
 *  file; it records nothing. */
function AuditViewer({ sheetId }: { sheetId: string }) {
  const [filter, setFilter] = useState('');

  const show = useMutation({
    mutationFn: (name: string) =>
      getJson<{ lines: string[] }>(
        `/api/sheets/${encodeURIComponent(sheetId)}/audit?name=${encodeURIComponent(name)}`,
      ),
  });

  return (
    <section data-testid="audit">
      <h2>Audit</h2>
      <input
        data-testid="audit-filter"
        placeholder="Person name (blank for all)"
        value={filter}
        onChange={(e) => {
          setFilter(e.target.value);
          show.reset();
        }}
      />{' '}
      <button
        type="button"
        data-testid="audit-show"
        disabled={show.isPending}
        onClick={() => {
          show.reset();
          show.mutate(filter);
        }}
      >
        Show
      </button>
      {show.isError && <span data-testid="audit-error">{show.error.message}</span>}
      {show.isSuccess && (
        <ul>
          {show.data.lines.map((line, i) => (
            <li key={i} data-testid="audit-line">
              {line}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export const contribution = SheetBody.fill({
  order: 90,
  Component: ({ sheetId }) => <AuditViewer sheetId={sheetId} />,
});
