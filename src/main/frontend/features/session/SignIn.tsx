import { useState } from 'react';
import { asString, useSearchParam } from '../../url/useSearchParam';

// Sign-in: a name, no password. The signed-in name lives in the URL (`user`), so it outlives a click.
export function SignIn() {
  const [, setUser] = useSearchParam('user', asString);
  const [name, setName] = useState('');
  return (
    <form
      data-testid="signin-form"
      onSubmit={(e) => {
        e.preventDefault();
        const trimmed = name.trim();
        if (trimmed !== '') setUser(trimmed);
      }}
    >
      <input data-testid="signin-name" value={name} onChange={(e) => setName(e.target.value)} placeholder="Your name" />
      <button type="submit" data-testid="signin-submit">Sign in</button>
    </form>
  );
}

export function CurrentUser() {
  const [user, setUser] = useSearchParam('user', asString);
  return (
    <p>
      Signed in as <span data-testid="current-user">{user}</span>{' '}
      <button type="button" data-testid="signout" onClick={() => setUser(undefined)}>Sign out</button>
    </p>
  );
}
