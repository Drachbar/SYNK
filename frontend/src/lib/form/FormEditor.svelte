<script lang="ts">
	import { send } from '#lib/api.ts';
	import {
		FIELD_TYPES,
		fieldsOf,
		newFieldId,
		type Content,
		type Field,
		type FieldType,
		type Form,
		type FormDefinition
	} from './form';

	type Props = {
		form: Form;
		/** Veckans svar, för att varna när ett fält med svar tas bort. */
		content: Content;
		onsaved: (form: Form) => void;
		oncancel: () => void;
	};
	let { form, content, onsaved, oncancel }: Props = $props();

	const original = (() => form.definition)();
	let draft = $state<FormDefinition>(structuredClone($state.snapshot(original)));
	let newType = $state<FieldType[]>(draft.sections.map(() => 'textarea'));
	let saving = $state(false);
	let error = $state<string | null>(null);

	const typeLabel = (type: FieldType) => FIELD_TYPES.find((t) => t.type === type)?.label ?? type;
	/** Frågan, eller avsnittets rubrik om frågan är tom. Söker i utkastet och sedan i originalet. */
	const fieldName = (f: Field) =>
		f.label ||
		[...draft.sections, ...original.sections].find((s) => s.fields.some((x) => x.id === f.id))
			?.title ||
		f.id;

	const checklists = $derived(fieldsOf(draft).filter((f) => f.type === 'checklist'));

	/** Fält med svar den här veckan som inte finns kvar i utkastet. */
	const lostAnswers = $derived.by(() => {
		const kept = new Set(fieldsOf(draft).map((f) => f.id));
		return fieldsOf(original).filter((f) => !kept.has(f.id) && content[f.id] !== undefined);
	});

	function addSection() {
		draft.sections.push({ title: '', fields: [] });
		newType.push('textarea');
	}

	function moveSection(i: number, delta: number) {
		const j = i + delta;
		if (j < 0 || j >= draft.sections.length) return;
		[draft.sections[i], draft.sections[j]] = [draft.sections[j], draft.sections[i]];
		[newType[i], newType[j]] = [newType[j], newType[i]];
	}

	function removeSection(i: number) {
		const s = draft.sections[i];
		if (s.fields.length && !confirm(`Ta bort avsnittet "${s.title}" och dess fält?`)) return;
		draft.sections.splice(i, 1);
		newType.splice(i, 1);
	}

	function addField(i: number) {
		draft.sections[i].fields.push({ id: newFieldId(), type: newType[i], label: '' });
	}

	/** Flyttar ett steg; från ett avsnitts början eller slut till föregående eller nästa avsnitt. */
	function moveField(si: number, fi: number, delta: number) {
		const fields = draft.sections[si].fields;
		const j = fi + delta;
		if (j >= 0 && j < fields.length) {
			[fields[fi], fields[j]] = [fields[j], fields[fi]];
			return;
		}
		const target = draft.sections[si + delta];
		if (!target) return;
		const [field] = fields.splice(fi, 1);
		if (delta < 0) target.fields.push(field);
		else target.fields.unshift(field);
	}

	function removeField(si: number, fi: number) {
		draft.sections[si].fields.splice(fi, 1);
	}

	async function save() {
		error = null;
		const ids = new Set(fieldsOf(draft).map((f) => f.id));
		const definition: FormDefinition = {
			sections: draft.sections.map((s) => ({
				title: s.title.trim(),
				fields: s.fields.map((f) => ({
					id: f.id,
					type: f.type,
					label: f.label.trim(),
					// Ett carryFrom till ett borttaget fält släpps.
					carryFrom:
						f.type === 'checklist' && f.carryFrom && ids.has(f.carryFrom) ? f.carryFrom : null
				}))
			}))
		};
		if (definition.sections.length === 0) {
			error = 'Formuläret behöver minst ett avsnitt.';
			return;
		}
		if (definition.sections.some((s) => s.title === '')) {
			error = 'Alla avsnitt behöver en rubrik.';
			return;
		}

		saving = true;
		try {
			const res = await send('/api/forms', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(definition)
			});
			if (res.status === 400) {
				error = `Formuläret är inte giltigt: ${(await res.json()).message}`;
				return;
			}
			if (!res.ok) {
				error = `Kunde inte spara formuläret (${res.status}). Försök igen.`;
				return;
			}
			onsaved(await res.json());
		} catch {
			error = 'Kunde inte nå servern. Försök igen.';
		} finally {
			saving = false;
		}
	}
</script>

<div class="editor">
	<p class="muted intro">
		Ändringarna sparas som en ny formulärsversion (nu version {form.version}). Den används för den
		här veckan när anteckningen sparas, och för alla nya veckor. Veckor som redan har en anteckning
		behåller sitt formulär.
	</p>

	{#each draft.sections as section, si (si)}
		<section>
			<div class="row">
				<input
					class="title"
					placeholder="Rubrik på avsnittet"
					aria-label="Rubrik på avsnittet"
					bind:value={section.title}
				/>
				<button type="button" aria-label="Flytta avsnittet upp" onclick={() => moveSection(si, -1)}
					>↑</button
				>
				<button type="button" aria-label="Flytta avsnittet ner" onclick={() => moveSection(si, 1)}
					>↓</button
				>
				<button type="button" aria-label="Ta bort avsnittet" onclick={() => removeSection(si)}
					>✕</button
				>
			</div>

			{#each section.fields as field, fi (field.id)}
				<div class="field">
					<div class="row">
						<input placeholder="Fråga (kan vara tom)" aria-label="Fråga" bind:value={field.label} />
						<button
							type="button"
							aria-label="Flytta fältet upp"
							onclick={() => moveField(si, fi, -1)}>↑</button
						>
						<button
							type="button"
							aria-label="Flytta fältet ner"
							onclick={() => moveField(si, fi, 1)}>↓</button
						>
						<button type="button" aria-label="Ta bort fältet" onclick={() => removeField(si, fi)}
							>✕</button
						>
					</div>
					<div class="meta muted">
						{typeLabel(field.type)}
						{#if field.type === 'checklist'}
							·
							<label class="inline">
								Fyll i från förra veckans
								<select
									bind:value={() => field.carryFrom ?? '', (v) => (field.carryFrom = v || null)}
								>
									<option value="">(ingenting)</option>
									{#each checklists as c (c.id)}
										<option value={c.id}>{fieldName(c)}</option>
									{/each}
								</select>
							</label>
						{/if}
					</div>
				</div>
			{/each}

			<div class="row add">
				<select aria-label="Typ av nytt fält" bind:value={newType[si]}>
					{#each FIELD_TYPES as t (t.type)}
						<option value={t.type}>{t.label}</option>
					{/each}
				</select>
				<button type="button" onclick={() => addField(si)}>+ Fält</button>
			</div>
		</section>
	{/each}

	<button type="button" onclick={addSection}>+ Nytt avsnitt</button>

	{#if lostAnswers.length}
		<p class="warn">
			Veckan har svar i {lostAnswers.map(fieldName).join(', ')}. De försvinner när anteckningen
			sparas med det nya formuläret.
		</p>
	{/if}

	<div class="actions">
		<span class="error" aria-live="polite">{error ?? ''}</span>
		<div>
			<button type="button" onclick={oncancel}>Avbryt</button>
			<button type="button" class="primary" disabled={saving} onclick={save}>
				{saving ? 'Sparar…' : 'Spara formuläret'}
			</button>
		</div>
	</div>
</div>

<style>
	.editor {
		display: grid;
		gap: 1rem;
	}

	.intro {
		margin: 0;
		font-size: 0.9rem;
	}

	section {
		display: grid;
		gap: 0.6rem;
		padding: 0.75rem;
		border: 1px solid var(--border);
		border-radius: var(--radius);
		background: var(--surface);
	}

	.row {
		display: flex;
		gap: 0.4rem;
		align-items: center;
	}

	.row button {
		flex: none;
		padding: 0.4rem 0.6rem;
	}

	input.title {
		font-weight: 600;
	}

	.field {
		padding-left: 1rem;
		border-left: 3px solid var(--border);
	}

	.meta {
		font-size: 0.85rem;
		margin-top: 0.2rem;
	}

	label.inline {
		display: inline;
		font-weight: normal;
	}

	select {
		font: inherit;
		color: inherit;
		background: var(--surface);
		border: 1px solid var(--border);
		border-radius: var(--radius);
		padding: 0.3rem 0.5rem;
	}

	.add {
		padding-left: 1rem;
	}

	.warn {
		background: var(--warn-bg);
		padding: 0.6rem 0.8rem;
		border-radius: var(--radius);
		margin: 0;
	}

	.actions {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 1rem;
	}

	.actions div {
		display: flex;
		gap: 0.5rem;
	}
</style>
