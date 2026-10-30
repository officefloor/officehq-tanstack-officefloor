import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { getJson, HttpError, postJson, putJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { asString, useSearchParam } from '../../url/useSearchParam';
import { GRADES, peopleKey, usePeople, type Person } from './usePeople';

// Adds a person, or — while a person's editor is open (`editPerson` in the URL) — saves that
// person. Opening the editor fills the form with the person's values and remembers the version it
// loaded; a save over someone else's newer save is refused and the form shows the current values.
export function PersonForm() {
  const [sheet] = useSearchParam('sheet', asString);
  const [user] = useSearchParam('user', asString);
  const [editParam, setEditing] = useSearchParam('editPerson', asString);
  const people = usePeople(sheet);
  const edited = people.find((p) => p.id === editParam);
  // A person not on this sheet (an id left from another sheet) has no editor open.
  const editing = edited?.id ?? '';
  const [name, setName] = useState('');
  const [grade, setGrade] = useState<string>('A');
  const [version, setVersion] = useState<number | undefined>(undefined);
  const queryClient = useQueryClient();
  const submitOnce = useSubmitOnce();

  const save = useMutation({
    mutationFn: (body: { name: string; grade: string }) =>
      editing !== ''
        ? putJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people/${encodeURIComponent(editing)}`, { ...body, user, version })
        : postJson<Person>(`/api/sheets/${encodeURIComponent(sheet)}/people`, { ...body, user }),
    onSuccess: () => {
      setName('');
      setGrade('A');
      if (editing !== '') setEditing(undefined);
      void queryClient.invalidateQueries({ queryKey: peopleKey(sheet) });
    },
    onError: async (error) => {
      if (!(error instanceof HttpError) || error.status !== 409 || editing === '') return;
      // Someone else saved this person since the editor loaded it: show what is stored now.
      const fresh = await queryClient.fetchQuery({
        queryKey: peopleKey(sheet),
        queryFn: () => getJson<Person[]>(`/api/sheets/${encodeURIComponent(sheet)}/people`),
        staleTime: 0,
      });
      const current = fresh.find((p) => p.id === editing);
      if (current) {
        setName(current.name);
        setGrade(current.grade);
        setVersion(current.version);
      }
    },
  });

  // Opening an editor fills the form with that person's values.
  const editedId = edited?.id;
  useEffect(() => {
    save.reset();
    if (edited) {
      setName(edited.name);
      setGrade(edited.grade);
      setVersion(edited.version);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editedId]);

  return (
    <form
      data-testid="person-form"
      onSubmit={(e) => {
        e.preventDefault();
        submitOnce(() => {
          save.reset();
          return save.mutateAsync({ name, grade });
        });
      }}
    >
      <input
        data-testid="person-form-name"
        value={name}
        placeholder="Name"
        onChange={(e) => {
          setName(e.target.value);
          save.reset();
        }}
      />{' '}
      <select
        data-testid="person-form-grade"
        value={grade}
        onChange={(e) => {
          setGrade(e.target.value);
          save.reset();
        }}
      >
        {GRADES.map((g) => (
          <option key={g} value={g}>{g}</option>
        ))}
      </select>{' '}
      <button type="submit" data-testid="person-form-submit">{editing !== '' ? 'Save' : 'Add person'}</button>
      {editing !== '' && (
        <button
          type="button"
          data-testid="person-form-cancel"
          onClick={() => {
            setEditing(undefined);
            setName('');
            setGrade('A');
            save.reset();
          }}
        >
          Cancel
        </button>
      )}
      {save.isError && (
        <span data-testid="person-form-error" role="alert">
          {save.error instanceof HttpError && save.error.status === 409
            ? ' Not saved: someone else changed this person since you opened it. The form now shows their values.'
            : ' Could not save: a name is required and must not already be in use.'}
        </span>
      )}
    </form>
  );
}
