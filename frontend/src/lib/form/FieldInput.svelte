<script lang="ts">
	import {
		WEEKDAYS,
		newItem,
		type Answer,
		type ChecklistItem,
		type Field,
		type User
	} from './form';

	type Props = { field: Field; users: User[]; value: Answer };
	let { field, users, value = $bindable() }: Props = $props();

	// Värdet har alltid rätt format för fälttypen (se toValues), så typerna kan smalnas av här.
	const text = () => value as string;
	const map = () => value as Record<string, string>;
	const items = () => value as ChecklistItem[];

	let newText = $state('');

	function addItem() {
		if (newText.trim() === '') return;
		items().push(newItem(newText.trim()));
		newText = '';
	}

	function removeItem(id: string) {
		value = items().filter((i) => i.id !== id);
	}
</script>

{#if field.type === 'text' || field.type === 'textarea'}
	<div class="field">
		{#if field.label}<label for={field.id}>{field.label}</label>{/if}
		{#if field.type === 'textarea'}
			<textarea
				id={field.id}
				aria-label={field.label || undefined}
				bind:value={text, (v) => (value = v)}></textarea>
		{:else}
			<input
				id={field.id}
				aria-label={field.label || undefined}
				bind:value={text, (v) => (value = v)}
			/>
		{/if}
	</div>
{:else}
	<fieldset class="field">
		{#if field.label}<legend>{field.label}</legend>{/if}

		{#if field.type === 'perPerson'}
			<div class="people">
				{#each users as user (user.id)}
					<div>
						<label class="sub" for="{field.id}-{user.id}">{user.name}</label>
						<textarea
							id="{field.id}-{user.id}"
							class="short"
							bind:value={() => map()[user.id] ?? '', (v) => (map()[user.id] = v)}></textarea>
					</div>
				{/each}
			</div>
		{:else if field.type === 'weekdays'}
			<div class="days">
				{#each WEEKDAYS as [day, name] (day)}
					<label class="sub" for="{field.id}-{day}">{name}</label>
					<input
						id="{field.id}-{day}"
						bind:value={() => map()[day] ?? '', (v) => (map()[day] = v)}
					/>
				{/each}
			</div>
		{:else if field.type === 'checklist'}
			<ul class="checklist">
				{#each items() as item (item.id)}
					<li class:done={item.done}>
						<input type="checkbox" bind:checked={item.done} aria-label="Klar" />
						<input class="item" bind:value={item.text} aria-label="Punkt" />
						<button
							type="button"
							class="remove"
							aria-label="Ta bort {item.text}"
							onclick={() => removeItem(item.id)}>✕</button
						>
					</li>
				{/each}
			</ul>
			<input
				class="new"
				placeholder="Ny punkt, tryck Enter"
				aria-label="Ny punkt"
				bind:value={newText}
				onkeydown={(e) => {
					if (e.key === 'Enter') {
						e.preventDefault();
						addItem();
					}
				}}
				onblur={addItem}
			/>
		{/if}
	</fieldset>
{/if}

<style>
	fieldset {
		border: none;
		margin: 0;
		padding: 0;
		min-width: 0;
	}

	legend {
		font-weight: 600;
		margin-bottom: 0.3rem;
		padding: 0;
	}

	.sub {
		font-weight: normal;
		color: var(--muted);
		margin-bottom: 0.15rem;
	}

	.people {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(15rem, 1fr));
		gap: 0.75rem;
	}

	textarea.short {
		min-height: 3.5rem;
	}

	.days {
		display: grid;
		grid-template-columns: max-content 1fr;
		align-items: center;
		gap: 0.4rem 0.75rem;
	}

	.days .sub {
		margin: 0;
	}

	.checklist {
		list-style: none;
		margin: 0 0 0.4rem;
		padding: 0;
	}

	.checklist li {
		display: flex;
		align-items: center;
		gap: 0.5rem;
	}

	.checklist input[type='checkbox'] {
		width: 1.2rem;
		height: 1.2rem;
		flex: none;
		accent-color: var(--accent);
	}

	input.item {
		border-color: transparent;
		background: transparent;
		padding: 0.3rem 0.4rem;
	}

	input.item:hover {
		border-color: var(--border);
	}

	li.done input.item {
		text-decoration: line-through;
		color: var(--muted);
	}

	.remove {
		border: none;
		background: none;
		color: var(--muted);
		padding: 0.2rem 0.4rem;
	}

	input.new {
		margin-left: 1.7rem;
		width: calc(100% - 1.7rem);
	}
</style>
