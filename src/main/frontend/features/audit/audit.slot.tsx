import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet } from '../../session/session';

// The audit viewer: the sheet's lines exactly as the audit file holds them, newest first. A name in the
// filter shows only the lines about that person (name=/person=, compared as names are); for a person
// transferred here, followed by the lines about them from the sheet they came from. It reads the
// file each time Show is pressed; it keeps no record of its own.
function AuditViewer() {
  const [sheet] = useCurrentSheet();
  const [filter, setFilter] = useState('');

  const show = useMutation({
    mutationFn: (person: string) =>
      getJson<string[]>(
        `/api/sheets/${encodeURIComponent(sheet)}/audit?person=${encodeURIComponent(person.trim())}`,
      ),
  });

  return (
    <section data-testid="audit-viewer">
      <h2>Audit</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          show.mutate(filter);
        }}
      >
        <input
          data-testid="audit-filter"
          placeholder="Person (blank for all)"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        />
        <button type="submit" data-testid="audit-show" disabled={sheet === ''}>
          Show
        </button>
        {show.isError && (
          <span data-testid="audit-error" role="alert">
            {' '}
            The audit could not be read.
          </span>
        )}
      </form>
      <ul>
        {(show.data ?? []).map((line, i) => (
          <li key={i} data-testid="audit-line">
            <code>{line}</code>
          </li>
        ))}
      </ul>
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 52, Component: AuditViewer });
