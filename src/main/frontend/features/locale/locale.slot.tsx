import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { LOCALES, localeKey, localePath, useLocale } from './locale';
import type { Locale, UserLocale } from './locale';

// The signed-in user's locale: how the screen shows them figures. Each user has their own (en unless chosen); it
// changes nothing on the sheet, so it is not audited.
function UserLocalePanel({ sheet, user }: { sheet: string; user: string }) {
  const queryClient = useQueryClient();
  const { data } = useLocale(sheet, user);
  const current = data?.locale ?? '';
  const [draft, setDraft] = useState<Locale | ''>('');

  const save = useMutation({
    mutationFn: (locale: Locale) => postJson<UserLocale>(byUser(localePath(sheet), user), { locale }),
    onSuccess: (updated) => {
      queryClient.setQueryData(localeKey(sheet, user), updated);
      setDraft('');
    },
  });
  const send = useSubmitOnce(save);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    send(draft === '' ? (current === '' ? 'en' : current) : draft);
  };

  return (
    <form onSubmit={submit}>
      <label>
        Locale{' '}
        <select
          data-testid="user-locale"
          value={draft === '' ? current : draft}
          onChange={(e) => {
            setDraft(e.target.value as Locale);
            save.reset();
          }}
        >
          {LOCALES.map((l) => (
            <option key={l} value={l}>
              {l}
            </option>
          ))}
        </select>
      </label>{' '}
      <button type="submit" data-testid="user-locale-save" disabled={save.isPending}>
        Save locale
      </button>{' '}
      now <span data-testid="user-locale-current">{current}</span>
      {save.isError && (
        <span data-testid="user-locale-error" role="alert">
          {' '}The locale must be en or de.
        </span>
      )}
    </form>
  );
}

export const contribution = SheetPanels.fill({ order: -9.5, Component: UserLocalePanel });
