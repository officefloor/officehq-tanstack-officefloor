import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/useSubmitOnce';
import { SheetBody } from '../../slots/defs/sheet';
import { useClaims } from '../claims/claims';
import { usePeople } from '../people/people';
import { personDueTotal, sumDue, useRates } from '../rates/rates';
import { useSheetToday } from '../sheet/sheet';
import { useCap, useFloor, useLate } from '../settings/settings';
import { crewsKey, useCrews } from './crews';
import type { Crew } from './crews';

/** A crew's due-total: its lead's and its members' due-totals (as shown on their rows) added up, skipping n/a. */
function useCrewDueTotal(sheetId: string, crew: Crew): string {
  const people = usePeople(sheetId).data ?? [];
  const claims = useClaims(sheetId).data ?? [];
  const rates = useRates(sheetId).data ?? [];
  const today = useSheetToday(sheetId);
  const floor = useFloor(sheetId);
  const late = useLate(sheetId);
  const cap = useCap(sheetId);
  const ids = new Set([crew.leadId, ...crew.members.map((m) => m.personId)]);
  return sumDue(people.filter((p) => ids.has(p.id)).map((p) => personDueTotal(p, claims, rates, today, floor, late, cap)));
}

/** One crew: its lead, its due-total, its members, and the entry a member is added with. */
function CrewRow({ sheetId, user, crew }: { sheetId: string; user: string; crew: Crew }) {
  const queryClient = useQueryClient();
  const people = usePeople(sheetId).data ?? [];
  const dueTotal = useCrewDueTotal(sheetId, crew);
  const [personId, setPersonId] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const add = useMutation({
    mutationFn: (id: number) =>
      postJson(`/api/sheets/${encodeURIComponent(sheetId)}/crews/${crew.id}/members`, { user, personId: id }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: crewsKey(sheetId) });
      setPersonId('');
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not add the crew member.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (personId === '') {
      setError('Choose the person to add.');
      return;
    }
    const id = Number(personId);
    if (id === crew.leadId) {
      setError('That person leads the crew.');
      return;
    }
    if (crew.members.some((m) => m.personId === id)) {
      setError('That person is already in the crew.');
      return;
    }
    if (!once.start()) return;
    add.mutate(id);
  };

  return (
    <li data-testid={`crew-row-${crew.name}`}>
      <strong>{crew.name}</strong> — lead <span data-testid="crew-lead">{crew.lead}</span>, due total{' '}
      <span data-testid="crew-due-total">{dueTotal}</span>
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
          value={personId}
          onChange={(e) => {
            setPersonId(e.target.value);
            setError('');
          }}
        >
          <option value="">Person…</option>
          {people.map((p) => (
            <option key={p.id} value={String(p.id)}>
              {p.name}
            </option>
          ))}
        </select>{' '}
        <button type="submit" data-testid="crew-member-add" disabled={add.isPending}>
          Add member
        </button>
      </form>
      {error && (
        <p data-testid="crew-error" role="alert">
          {error}
        </p>
      )}
    </li>
  );
}

function CrewForm({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const people = usePeople(sheetId).data ?? [];
  const [name, setName] = useState('');
  const [leadId, setLeadId] = useState('');
  const [error, setError] = useState('');
  const once = useSubmitOnce();

  const save = useMutation({
    mutationFn: (body: { name: string; leadId: number }) =>
      postJson<Crew>(`/api/sheets/${encodeURIComponent(sheetId)}/crews`, { user, ...body }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: crewsKey(sheetId) });
      setName('');
      setLeadId('');
    },
    onError: (e) => setError(e instanceof HttpError && e.body ? e.body : 'Could not add the crew.'),
    onSettled: once.done,
  });

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (name.trim() === '') {
      setError('Give the crew a name.');
      return;
    }
    if (leadId === '') {
      setError("Choose the crew's lead.");
      return;
    }
    if (!once.start()) return;
    save.mutate({ name: name.trim(), leadId: Number(leadId) });
  };

  return (
    <form data-testid="crew-form" onSubmit={submit}>
      <input
        data-testid="crew-form-name"
        placeholder="Crew name"
        value={name}
        onChange={(e) => {
          setName(e.target.value);
          setError('');
        }}
      />{' '}
      <select
        data-testid="crew-form-lead"
        value={leadId}
        onChange={(e) => {
          setLeadId(e.target.value);
          setError('');
        }}
      >
        <option value="">Lead…</option>
        {people.map((p) => (
          <option key={p.id} value={String(p.id)}>
            {p.name}
          </option>
        ))}
      </select>{' '}
      <button type="submit" data-testid="crew-form-submit" disabled={save.isPending}>
        Add crew
      </button>
      {error && (
        <p data-testid="crew-form-error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

/** The sheet's crews: each with its lead, its due-total and its members, and the entry a crew is added with. */
function Crews({ sheetId, user }: { sheetId: string; user: string }) {
  const crews = useCrews(sheetId).data ?? [];
  return (
    <section data-testid="crews">
      <h2>Crews</h2>
      <CrewForm sheetId={sheetId} user={user} />
      <ul>
        {crews.map((c) => (
          <CrewRow key={c.id} sheetId={sheetId} user={user} crew={c} />
        ))}
      </ul>
    </section>
  );
}

export const contribution = SheetBody.fill({ order: 7.85, Component: Crews });
