<script lang="ts">
	type TestResponse = { message: string };

	let result = $state<string | null>(null);
	let error = $state<string | null>(null);
	let loading = $state(false);

	async function callTestApi() {
		loading = true;
		error = null;
		result = null;
		try {
			const res = await fetch('/api/test');
			if (!res.ok) {
				throw new Error(`${res.status} ${res.statusText}`);
			}
			const data: TestResponse = await res.json();
			result = data.message;
		} catch (e) {
			error = e instanceof Error ? e.message : String(e);
		} finally {
			loading = false;
		}
	}
</script>

<h1>SYNK</h1>

<button onclick={callTestApi} disabled={loading}>
	{loading ? 'Anropar…' : 'Anropa /api/test'}
</button>

{#if result}
	<p>Svar: {result}</p>
{/if}
{#if error}
	<p style="color: red">Fel: {error}</p>
{/if}
