import { useState } from 'react';
import type { FormEvent } from 'react';
import { AppSession } from '../../slots/defs/appSession';
import { useCurrentUser } from './session';

function SignIn() {
  const [user, setUser] = useCurrentUser();
  const [name, setName] = useState('');
  const [error, setError] = useState('');

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) {
      setError('Type a name to sign in.');
      return;
    }
    setError('');
    setUser(trimmed);
  };

  if (user) {
    return (
      <span data-testid="signed-in">
        Signed in as <strong data-testid="current-user">{user}</strong>
      </span>
    );
  }
  return (
    <form data-testid="signin-form" onSubmit={submit}>
      <input
        data-testid="signin-name"
        placeholder="Your name"
        value={name}
        onChange={(e) => {
          setName(e.target.value);
          setError('');
        }}
      />
      <button type="submit" data-testid="signin-submit">
        Sign in
      </button>
      {error && <span data-testid="signin-error">{error}</span>}
    </form>
  );
}

export const contribution = AppSession.fill({ order: 0, Component: SignIn });
