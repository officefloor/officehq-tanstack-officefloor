import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOnce } from '../../query/useOnce';
import { postJson } from '../../api/http';
import { SheetBody } from '../../slots/defs/sheetBody';
import { useUser } from '../session/session';
import { usePeople } from '../people/people';
import { crewDueTotal, crewsKey, useCrews, useDueTotals } from './crews';
import type { Crew } from './crews';

// The sheet's crews: a name entry, a lead chosen from the people and an add button, each crew listed below
// with its lead, its members and its due-total (the lead's and the members' due-totals added up, n/a
// skipped). A member is added from the crew's row. The server decides what is refused — a taken or blank
// name, or a member that is the lead or already in the crew — and the form shows its error.
function Crews({ sheetId }: { sheetId: string }) {
  const queryClient = useQueryClient();
  const crews = useCrews(sheetId);
  const people = usePeople(sheetId);
  const dues = useDueTotals(sheetId);
  const [name, setName] = useState('');
  const [leadId, setLeadId] = useState('');
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const add = useMutation({
    mutationFn: (body: { name: string; leadId: string }) =>
      postJson<Crew>(`/api/sheets/${encodeURIComponent(sheetId)}/crews`, body, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: crewsKey(sheetId) }),
    onError: () => setError('That crew could not be added.'),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    once((done) => add.mutate({ name, leadId }, { onSettled: done }));
  };

  return (
    <section data-testid="crews">
      <h2>Crews</h2>
      <form onSubmit={submit}>
        <label>
          Name{' '}
          <input
            data-testid="crew-form-name"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              setError('');
            }}
          />
        </label>{' '}
        <select
          data-testid="crew-form-lead"
          aria-label="Lead"
          value={leadId}
          onChange={(e) => {
            setLeadId(e.target.value);
            setError('');
          }}
        >
          <option value="">Lead…</option>
          {(people.data ?? []).map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="crew-form-submit" disabled={add.isPending}>
          Add crew
        </button>
        {error && (
          <p role="alert" data-testid="crew-form-error">
            {error}
          </p>
        )}
      </form>
      <ul data-testid="crew-list">
        {(crews.data ?? []).map((c) => (
          <CrewRow key={c.name} sheetId={sheetId} crew={c} dues={dues} />
        ))}
      </ul>
    </section>
  );
}

function CrewRow({ sheetId, crew, dues }: { sheetId: string; crew: Crew; dues: Map<string, string> }) {
  const queryClient = useQueryClient();
  const people = usePeople(sheetId);
  const [personId, setPersonId] = useState('');
  const [error, setError] = useState('');
  const [user] = useUser();

  const once = useOnce();
  const add = useMutation({
    mutationFn: (body: { crew: string; personId: string }) =>
      postJson(`/api/sheets/${encodeURIComponent(sheetId)}/crew-members`, body, user),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: crewsKey(sheetId) }),
    onError: () => setError(`That member could not be added to ${crew.name}.`),
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    once((done) => add.mutate({ crew: crew.name, personId }, { onSettled: done }));
  };

  const total = crewDueTotal(crew, dues);

  return (
    <li data-testid={`crew-row-${crew.name}`}>
      <strong>{crew.name}</strong> — lead <span data-testid="crew-lead">{crew.lead}</span>, due{' '}
      <span data-testid="crew-due-total">{total}</span>
      <ul>
        {crew.members.map((m) => (
          <li key={m.personId} data-testid="crew-member-line">
            {m.name}
          </li>
        ))}
      </ul>
      <form onSubmit={submit}>
        <select
          data-testid="crew-member-select"
          aria-label="Member"
          value={personId}
          onChange={(e) => {
            setPersonId(e.target.value);
            setError('');
          }}
        >
          <option value="">Person…</option>
          {(people.data ?? []).map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="crew-member-add" disabled={add.isPending}>
          Add member
        </button>
        {error && (
          <p role="alert" data-testid="crew-error">
            {error}
          </p>
        )}
      </form>
    </li>
  );
}

export const contribution = SheetBody.fill({ order: -5.25, Component: Crews });
