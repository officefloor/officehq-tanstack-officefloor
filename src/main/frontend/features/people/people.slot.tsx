import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson, putJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { SheetPage } from '../../slots/defs/sheetPage';
import { GRADES, peopleKey, useEditingPerson, usePeople, type Person } from './people';

type Values = { name: string; grade: string };
const EMPTY: Values = { name: '', grade: 'A' };

/** The sheet's people: a form that adds a person or saves the one being edited, and the list. */
function People({ sheetId, user }: { sheetId: string; user: string }) {
  const people = usePeople(sheetId);
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useEditingPerson();
  const editing = people.data?.find((p) => String(p.id) === editingId);
  const [values, setValues] = useState<Values>(EMPTY);

  const save = useMutation({
    mutationFn: ({ id, ...v }: Values & { id?: number }) => {
      const base = `/api/sheets/${encodeURIComponent(sheetId)}/people`;
      const body = { ...v, by: user };
      return id === undefined ? postJson<Person>(base, body) : putJson<Person>(`${base}/${id}`, body);
    },
    onSuccess: () => {
      setValues(EMPTY);
      setEditingId(undefined);
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) });
    },
  });

  const change = (v: Partial<Values>) => {
    setValues((was) => ({ ...was, ...v }));
    save.reset();
  };
  const openEditor = (p: Person) => {
    save.reset();
    setValues({ name: p.name, grade: p.grade });
    setEditingId(String(p.id));
  };
  const cancel = () => {
    save.reset();
    setValues(EMPTY);
    setEditingId(undefined);
  };

  return (
    <section data-testid="people">
      <h3>People</h3>
      <form
        data-testid="person-form"
        onSubmit={(e) => {
          e.preventDefault();
          save.reset();
          save.mutate({ ...values, id: editing?.id });
        }}
      >
        <label>
          Name{' '}
          <input data-testid="person-form-name" value={values.name} onChange={(e) => change({ name: e.target.value })} />
        </label>{' '}
        <label>
          Grade{' '}
          <select data-testid="person-form-grade" value={values.grade} onChange={(e) => change({ grade: e.target.value })}>
            {GRADES.map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </label>{' '}
        <button type="submit" data-testid="person-form-submit" disabled={save.isPending}>
          {editing ? 'Save' : 'Add person'}
        </button>
        {editing && (
          <button type="button" data-testid="person-form-cancel" onClick={cancel}>
            Cancel
          </button>
        )}
        {save.isError && (
          <span data-testid="person-form-error">
            {values.name.trim() === ''
              ? 'A name is required.'
              : save.error instanceof HttpError && save.error.status === 409
                ? 'That name is already in use on this sheet.'
                : 'Could not save this person.'}
          </span>
        )}
      </form>
      {people.isError && <p data-testid="people-load-error">Could not load people.</p>}
      <table data-testid="people-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Grade</th>
            <PeopleHead.Slot sheetId={sheetId} user={user} />
            <th />
          </tr>
        </thead>
        <tbody>
          {people.data?.map((p) => (
            <tr key={p.id} data-testid={`person-row-${p.id}`}>
              <td data-testid="person-name">{p.name}</td>
              <td data-testid="person-grade">{p.grade}</td>
              <PersonRow.Slot sheetId={sheetId} user={user} person={p} />
              <td>
                <button type="button" data-testid={`person-edit-${p.id}`} onClick={() => openEditor(p)}>
                  Edit
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export const contribution = SheetPage.fill({ order: 10, Component: People });
