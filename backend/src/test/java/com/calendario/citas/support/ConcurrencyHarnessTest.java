package com.calendario.citas.support;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba unitaria (sin Spring) del propio {@link ConcurrencyHarness}: es el
 * andamiaje del nivel de tests de concurrencia, así que se verifica por sí mismo.
 */
class ConcurrencyHarnessTest {

	@Test
	void runsEveryActionAndCollectsResults() throws InterruptedException {
		int threads = 32;
		AtomicInteger counter = new AtomicInteger();

		ConcurrencyHarness.Result<Integer> result =
				ConcurrencyHarness.runInParallel(threads, counter::incrementAndGet);

		assertThat(result.successCount()).isEqualTo(threads);
		assertThat(result.failureCount()).isZero();
		assertThat(counter).hasValue(threads);
	}

	@Test
	void capturesFailuresPerThread() throws InterruptedException {
		int threads = 10;

		ConcurrencyHarness.Result<Object> result = ConcurrencyHarness.runInParallel(threads, () -> {
			throw new IllegalStateException("boom");
		});

		assertThat(result.successCount()).isZero();
		assertThat(result.failureCount()).isEqualTo(threads);
		assertThat(result.failuresOfType(IllegalStateException.class)).isEqualTo(threads);
	}

	@Test
	void reportsWhenExactlyOneSucceeded() throws InterruptedException {
		AtomicInteger attempts = new AtomicInteger();

		ConcurrencyHarness.Result<String> result = ConcurrencyHarness.runInParallel(8, () -> {
			if (attempts.getAndIncrement() == 0) {
				return "winner";
			}
			throw new IllegalStateException("already taken");
		});

		assertThat(result.exactlyOneSucceeded()).isTrue();
		assertThat(result.successes()).containsExactly("winner");
		assertThat(result.failureCount()).isEqualTo(7);
	}
}
