<script lang="ts">
	import { resolve } from '$app/paths';
	import { beforeNavigate, goto, invalidateAll } from '$app/navigation';
	import { formatTimestamp, send, type EntryResponse } from '#lib/api.ts';
	import type { Week } from '#lib/week.ts';
	import { SCHEMA_VERSION, fieldsFor, type EntryContent } from './fields';

	type Props = { week: Week; entry: EntryResponse | null };
	let { week, entry }: Props = $props();

	// Formuläret startar från det som hämtades; sidan skapar en ny komponent när det ändras.
	const initial = (() => entry)();

	let values = $state<EntryContent>({ ...initial?.content });
	let saved = $state<EntryContent>({ ...initial?.content });
	let version = $state<number | null>(initial?.version ?? null);
	let updatedBy = $state(initial?.updatedBy);
	let updatedAt = $state(initial?.updatedAt);

	let saving = $state(false);
	let error = $state<string | null>(null);
	let conflict = $state(false);
	let justSaved = $state(false);

	/** Tomma svar sparas inte; ett fält som saknas betyder "inget svar". */
	function answers(content: EntryContent): EntryContent {
		return Object.fromEntries(Object.entries(content).filter(([, v]) => v.trim() !== ''));
	}

	/** Jämför nyckel för nyckel; jsonb returnerar inte nycklarna i samma ordning som de skickades. */
	function sameAnswers(a: EntryContent, b: EntryContent): boolean {
		const x = answers(a);
		const y = answers(b);
		const keys = Object.keys(x);
		return keys.length === Object.keys(y).length && keys.every((k) => x[k] === y[k]);
	}

	const dirty = $derived(!sameAnswers(values, saved));
	// Fälten bestäms av det sparade innehållet, så att ett avaktiverat fält inte försvinner medan man skriver.
	const visibleFields = $derived(fieldsFor(saved));

	async function save() {
		if (!dirty || saving) return;
		saving = true;
		error = null;
		justSaved = false;
		try {
			const content = answers(values);
			const res = await send(`/api/entries/${week.year}/${week.week}`, {
				method: 'PUT',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ version, schemaVersion: SCHEMA_VERSION, content })
			});
			if (res.status === 409) {
				conflict = true;
				return;
			}
			if (res.status === 401) {
				error = 'Du har blivit utloggad. Kopiera det du skrivit och logga in igen.';
				return;
			}
			if (!res.ok) {
				error = `Kunde inte spara (${res.status}).`;
				return;
			}
			const result: EntryResponse = await res.json();
			saved = { ...result.content };
			version = result.version;
			updatedBy = result.updatedBy;
			updatedAt = result.updatedAt;
			justSaved = true;
		} catch {
			error = 'Kunde inte nå servern. Försök igen.';
		} finally {
			saving = false;
		}
	}

	function onKeydown(event: KeyboardEvent) {
		if ((event.ctrlKey || event.metaKey) && event.key === 's') {
			event.preventDefault();
			save();
		}
	}

	beforeNavigate((navigation) => {
		if (!dirty) return;
		if (navigation.willUnload) {
			// Webbläsaren visar sin egen fråga.
			navigation.cancel();
			return;
		}
		if (!confirm('Du har osparade ändringar. Lämna veckan ändå?')) {
			navigation.cancel();
		}
	});

	async function reload() {
		// Ändringarna kastas medvetet; användaren har fått kopiera dem först.
		saved = { ...values };
		await invalidateAll();
	}

	async function relogin() {
		saved = { ...values };
		await goto(resolve('/login'), { invalidateAll: true });
	}
</script>

<svelte:window onkeydown={onKeydown} />

<form
	onsubmit={(e) => {
		e.preventDefault();
		save();
	}}
>
	{#each visibleFields as field (field.id)}
		<div class="field">
			<label for={field.id}>{field.label}</label>
			{#if field.type === 'textarea'}
				<textarea
					id={field.id}
					bind:value={() => values[field.id] ?? '', (v) => (values[field.id] = v)}></textarea>
			{:else}
				<input
					id={field.id}
					type={field.type}
					bind:value={() => values[field.id] ?? '', (v) => (values[field.id] = v)}
				/>
			{/if}
		</div>
	{/each}

	<div class="actions">
		<div class="status" aria-live="polite">
			{#if conflict}
				<span class="error">
					Anteckningen har sparats av någon annan sedan du öppnade den. Kopiera det du skrivit, och
					<button type="button" class="link" onclick={reload}>hämta den senaste versionen</button>.
				</span>
			{:else if error}
				<span class="error">
					{error}
					{#if error.includes('utloggad')}
						<button type="button" class="link" onclick={relogin}>Logga in</button>
					{/if}
				</span>
			{:else if dirty}
				<span class="muted">Osparade ändringar</span>
			{:else if justSaved}
				<span class="muted">Sparat ✓</span>
			{:else if updatedBy && updatedAt}
				<span class="muted">Senast sparad av {updatedBy}, {formatTimestamp(updatedAt)}</span>
			{:else}
				<span class="muted">Ingen anteckning för veckan än</span>
			{/if}
		</div>
		<button class="primary" disabled={!dirty || saving || conflict}>
			{saving ? 'Sparar…' : 'Spara'}
		</button>
	</div>
</form>

<style>
	form {
		display: grid;
		gap: 1.25rem;
	}

	.actions {
		position: sticky;
		bottom: 0;
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 1rem;
		padding: 0.75rem 0;
		background: var(--bg);
		border-top: 1px solid var(--border);
	}

	.status {
		font-size: 0.9rem;
	}

	.link {
		border: none;
		background: none;
		padding: 0;
		color: var(--accent);
		text-decoration: underline;
	}
</style>
