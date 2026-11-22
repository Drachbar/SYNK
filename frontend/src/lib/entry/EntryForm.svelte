<script lang="ts">
	import { resolve } from '$app/paths';
	import { beforeNavigate, goto, invalidateAll } from '$app/navigation';
	import { formatTimestamp, send, type EntryResponse } from '#lib/api.ts';
	import type { Week } from '#lib/week.ts';
	import FieldInput from '#lib/form/FieldInput.svelte';
	import FormEditor from '#lib/form/FormEditor.svelte';
	import {
		carriedOver,
		fieldsOf,
		moveUndone,
		sameContent,
		toContent,
		toValues,
		type ChecklistItem,
		type Content,
		type Field,
		type Form,
		type User
	} from '#lib/form/form.ts';

	type Props = {
		week: Week;
		entry: EntryResponse | null;
		form: Form;
		users: User[];
		/** Förra anteckningen, bara när veckan inte har någon anteckning än. */
		previous: EntryResponse | null;
	};
	let props: Props = $props();

	// Formuläret startar från det som hämtades; sidan skapar en ny komponent när det ändras.
	const initial = (() => ({ ...props }))();
	const start: Content =
		initial.entry?.content ??
		carriedOver(initial.form.definition, initial.previous?.content ?? null);

	let form = $state(initial.form);
	let values = $state(toValues(initial.form.definition, start));
	// Det förifyllda räknas inte som en ändring: lämnar man veckan orörd sparas ingenting.
	let saved = $state(toContent(initial.form.definition, toValues(initial.form.definition, start)));
	let savedFormVersion = $state(initial.entry?.formVersion ?? initial.form.version);
	let version = $state<number | null>(initial.entry?.version ?? null);
	let updatedBy = $state(initial.entry?.updatedBy);
	let updatedAt = $state(initial.entry?.updatedAt);

	let editing = $state(false);
	let saving = $state(false);
	let error = $state<string | null>(null);
	let conflict = $state(false);
	let justSaved = $state(false);
	let notice = $state<string | null>(null);

	const content = $derived(toContent(form.definition, values));
	const dirty = $derived(form.version !== savedFormVersion || !sameContent(content, saved));
	const fieldById = $derived(new Map(fieldsOf(form.definition).map((f) => [f.id, f])));

	async function save() {
		if (!dirty || saving || editing) return;
		saving = true;
		error = null;
		justSaved = false;
		try {
			const res = await send(`/api/entries/${props.week.year}/${props.week.week}`, {
				method: 'PUT',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ version, formVersion: form.version, content })
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
			saved = result.content;
			savedFormVersion = result.formVersion;
			version = result.version;
			updatedBy = result.updatedBy;
			updatedAt = result.updatedAt;
			justSaved = true;
			notice = null;
		} catch {
			error = 'Kunde inte nå servern. Försök igen.';
		} finally {
			saving = false;
		}
	}

	function formSaved(next: Form) {
		const before = new Set(fieldsOf(form.definition).map((f) => f.id));
		// Svar i fält som finns kvar behålls. Nya checklistor förifylls från förra anteckningen.
		const carried =
			version === null
				? carriedOver(next.definition, props.previous?.content ?? null, (f) => !before.has(f.id))
				: {};
		values = toValues(next.definition, { ...carried, ...content });
		form = next;
		editing = false;
	}

	function moveToTarget(field: Field) {
		const target = field.carryFrom ? fieldById.get(field.carryFrom) : undefined;
		if (!target) return;
		const moved = moveUndone(
			values[field.id] as ChecklistItem[],
			values[target.id] as ChecklistItem[]
		);
		notice =
			moved === 0
				? 'Inget att flytta.'
				: `Flyttade ${moved} ${moved === 1 ? 'punkt' : 'punkter'} till ${sectionTitle(target)}.`;
	}

	function sectionTitle(field: Field): string {
		return (
			field.label ||
			form.definition.sections.find((s) => s.fields.some((f) => f.id === field.id))?.title ||
			''
		);
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
		saved = content;
		savedFormVersion = form.version;
		await invalidateAll();
	}

	async function relogin() {
		saved = content;
		savedFormVersion = form.version;
		await goto(resolve('/login'), { invalidateAll: true });
	}
</script>

<svelte:window onkeydown={onKeydown} />

{#if editing}
	<FormEditor {form} {content} onsaved={formSaved} oncancel={() => (editing = false)} />
{:else}
	<form
		onsubmit={(e) => {
			e.preventDefault();
			save();
		}}
	>
		{#each form.definition.sections as section, si (si)}
			<section>
				<h2>{section.title}</h2>
				{#each section.fields as field (field.id)}
					<FieldInput {field} users={props.users} bind:value={values[field.id]} />
					{#if field.type === 'checklist' && field.carryFrom && fieldById.has(field.carryFrom) && field.carryFrom !== field.id}
						<div class="move">
							<button type="button" class="link" onclick={() => moveToTarget(field)}>
								Flytta det som inte blev gjort till {sectionTitle(fieldById.get(field.carryFrom)!)}
							</button>
						</div>
					{/if}
				{/each}
			</section>
		{/each}

		<p class="formversion muted">
			Formulär version {form.version}{form.version !== savedFormVersion
				? ' (ny, inte sparad än)'
				: ''}
			·
			<button type="button" class="link" onclick={() => (editing = true)}>Ändra formuläret</button>
		</p>

		<div class="actions">
			<div class="status" aria-live="polite">
				{#if conflict}
					<span class="error">
						Anteckningen har sparats av någon annan sedan du öppnade den. Kopiera det du skrivit,
						och
						<button type="button" class="link" onclick={reload}>hämta den senaste versionen</button
						>.
					</span>
				{:else if error}
					<span class="error">
						{error}
						{#if error.includes('utloggad')}
							<button type="button" class="link" onclick={relogin}>Logga in</button>
						{/if}
					</span>
				{:else if notice && dirty}
					<span class="muted">{notice} Osparade ändringar</span>
				{:else if dirty}
					<span class="muted">Osparade ändringar</span>
				{:else if justSaved}
					<span class="muted">Sparat ✓</span>
				{:else if updatedBy && updatedAt}
					<span class="muted">Senast sparad av {updatedBy}, {formatTimestamp(updatedAt)}</span>
				{:else if props.previous && Object.keys(saved).length}
					<span class="muted">Ingen anteckning än; förifylld från v.{props.previous.isoWeek}</span>
				{:else}
					<span class="muted">Ingen anteckning för veckan än</span>
				{/if}
			</div>
			<button class="primary" disabled={!dirty || saving || conflict}>
				{saving ? 'Sparar…' : 'Spara'}
			</button>
		</div>
	</form>
{/if}

<style>
	form {
		display: grid;
		gap: 2rem;
	}

	section {
		display: grid;
		gap: 1rem;
	}

	h2 {
		margin: 0;
		font-size: 1.2rem;
		padding-bottom: 0.3rem;
		border-bottom: 1px solid var(--border);
	}

	.move {
		margin-top: -0.5rem;
		font-size: 0.9rem;
	}

	.formversion {
		margin: 0;
		font-size: 0.85rem;
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
		font-size: inherit;
	}
</style>
