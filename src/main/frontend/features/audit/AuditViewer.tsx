import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';

// The audit viewer: the sheet's audit lines exactly as the file holds them, newest first — or, with a
// name in the filter, only the lines about that person (name= or person=, compared as names are). It
// reads the file on each show; it is not a second record. A new show clears the old list first. For a person
// transferred in, the lines about them from the sheet they came from follow, newest first.
export function AuditViewer() {
  const [sheet] = useSearchParam('sheet', asString);
  const [filter, setFilter] = useState('');
  const show = useMutation({
    mutationFn: (name: string) =>
      getJson<string[]>(`/api/sheets/${encodeURIComponent(sheet)}/audit?name=${encodeURIComponent(name)}`),
  });
  return (
    <section>
      <form
        data-testid="audit-form"
        onSubmit={(e) => {
          e.preventDefault();
          show.reset();
          show.mutate(filter);
        }}
      >
        <input
          data-testid="audit-filter"
          value={filter}
          placeholder="Person (blank for all)"
          onChange={(e) => {
            setFilter(e.target.value);
            show.reset();
          }}
        />{' '}
        <button type="submit" data-testid="audit-show">Show audit</button>
        {show.isError && <span data-testid="audit-error" role="alert"> Could not read the audit.</span>}
      </form>
      <ul data-testid="audit-list">
        {(show.data ?? []).map((line, i) => (
          <li key={i} data-testid="audit-line">{line}</li>
        ))}
      </ul>
    </section>
  );
}
