import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { AppHeader } from '../../slots/defs/appHeader';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { LOCALES, type Locale, localeKey, useLocale } from './locale';

// The signed-in user's locale: how the screen shows them figures. Saved per user (on the sheet), not audited.
function UserLocale() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const current = useLocale(sheet, user);
  const [choice, setChoice] = useState<Locale | ''>('');
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (locale: Locale) =>
      postJson<{ locale: Locale }>(`/api/sheets/${encodeURIComponent(sheet)}/locale`, { user, locale }),
    onSuccess: (saved) => {
      queryClient.setQueryData(localeKey(sheet, user), saved);
      setChoice('');
    },
  });
  const submit = useSubmitOnce(save);

  if (!user || !sheet || !current.data) return null;
  const selected = choice || current.data.locale;
  return (
    <form
      data-testid="user-locale-form"
      onSubmit={(e) => {
        e.preventDefault();
        submit(selected);
      }}
    >
      Locale <strong data-testid="user-locale-current">{current.data.locale}</strong>{' '}
      <select
        data-testid="user-locale"
        value={selected}
        onChange={(e) => {
          setChoice(e.target.value as Locale);
          save.reset();
        }}
      >
        {LOCALES.map((l) => (
          <option key={l} value={l}>
            {l}
          </option>
        ))}
      </select>{' '}
      <button type="submit" data-testid="user-locale-save" disabled={save.isPending}>
        Save locale
      </button>
      {save.isError && (
        <span data-testid="user-locale-error" role="alert">
          {' '}
          {save.error instanceof HttpError && save.error.reason ? `${save.error.reason}.` : 'The locale was refused.'}
        </span>
      )}
    </form>
  );
}

export const contribution = AppHeader.fill({ order: 15, Component: UserLocale });
