<script lang="ts">
	import { resolve } from '$app/paths';
	import { goto } from '$app/navigation';
	import { send } from '#lib/api.ts';

	let username = $state('');
	let password = $state('');
	let error = $state<string | null>(null);
	let busy = $state(false);

	async function login(event: SubmitEvent) {
		event.preventDefault();
		busy = true;
		error = null;
		try {
			const res = await send('/api/auth/login', {
				method: 'POST',
				body: new URLSearchParams({ username, password })
			});
			if (res.status === 401) {
				error = 'Fel namn eller lösenord.';
				return;
			}
			if (!res.ok) {
				error = `Något gick fel (${res.status}). Ladda om sidan och försök igen.`;
				return;
			}
			// invalidateAll kör om layoutens /api/auth/me, som också hämtar den nya CSRF-kakan.
			await goto(resolve('/'), { invalidateAll: true });
		} catch {
			error = 'Kunde inte nå servern.';
		} finally {
			busy = false;
		}
	}
</script>

<form onsubmit={login}>
	<h1>SYNK</h1>
	<p class="muted">Summering, Yrkanden/Yttranden, Nästa vecka, Käk</p>

	<div>
		<label for="username">Namn</label>
		<input id="username" bind:value={username} autocomplete="username" required />
	</div>
	<div>
		<label for="password">Lösenord</label>
		<input
			id="password"
			type="password"
			bind:value={password}
			autocomplete="current-password"
			required
		/>
	</div>

	{#if error}
		<p class="error" role="alert">{error}</p>
	{/if}

	<button class="primary" disabled={busy}>{busy ? 'Loggar in…' : 'Logga in'}</button>
</form>

<style>
	form {
		max-width: 22rem;
		margin: 10vh auto 0;
		display: grid;
		gap: 1rem;
	}

	h1 {
		margin: 0;
		letter-spacing: 0.08em;
	}

	p {
		margin: 0;
	}
</style>
