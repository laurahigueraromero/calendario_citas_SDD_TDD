package com.calendario.citas.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Utilidad para ejecutar la misma acción desde varios hilos <em>a la vez</em> y
 * recoger, por separado, los resultados correctos y las excepciones.
 *
 * <p>Todos los hilos se quedan esperando en una barrera y se liberan en el mismo
 * instante, de modo que la ventana de concurrencia sea lo más estrecha posible.
 * Es la base de los escenarios de verificación de concurrencia del PRD
 * (VC-1, VC-2, VC-3).
 */
public final class ConcurrencyHarness {

	/** Tiempo máximo que se espera a que terminen todas las acciones. */
	public static final long TIMEOUT_SECONDS = 30;

	private ConcurrencyHarness() {
	}

	/**
	 * Lanza {@code threads} hilos que invocan {@code action} simultáneamente.
	 *
	 * @return los valores devueltos por las invocaciones que terminaron sin
	 *         excepción y las excepciones (o errores) de las que fallaron.
	 */
	public static <T> Result<T> runInParallel(int threads, Callable<T> action) throws InterruptedException {
		if (threads < 1) {
			throw new IllegalArgumentException("threads debe ser >= 1");
		}

		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch startGate = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);
		List<T> successes = Collections.synchronizedList(new ArrayList<>());
		List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

		try {
			for (int i = 0; i < threads; i++) {
				pool.submit(() -> {
					ready.countDown();
					try {
						startGate.await();
						successes.add(action.call());
					}
					catch (Throwable t) {
						failures.add(t);
					}
					finally {
						done.countDown();
					}
				});
			}

			ready.await();          // todos los hilos parados en la barrera
			startGate.countDown();  // se liberan a la vez

			if (!done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				throw new IllegalStateException(
						"Las acciones concurrentes no terminaron en " + TIMEOUT_SECONDS + "s");
			}
		}
		finally {
			pool.shutdownNow();
		}

		return new Result<>(List.copyOf(successes), List.copyOf(failures));
	}

	/** Variante para acciones sin valor de retorno. */
	public static Result<Void> runInParallel(int threads, Runnable action) throws InterruptedException {
		return runInParallel(threads, () -> {
			action.run();
			return null;
		});
	}

	/**
	 * Resultado agregado de una ejecución concurrente.
	 *
	 * @param successes valores devueltos por las invocaciones correctas
	 * @param failures  excepciones/errores de las invocaciones que fallaron
	 */
	public record Result<T>(List<T> successes, List<Throwable> failures) {

		public int successCount() {
			return successes.size();
		}

		public int failureCount() {
			return failures.size();
		}

		/** {@code true} si exactamente una invocación tuvo éxito. */
		public boolean exactlyOneSucceeded() {
			return successCount() == 1;
		}

		/** Número de fallos cuya excepción es (o está causada por) {@code type}. */
		public long failuresOfType(Class<? extends Throwable> type) {
			return failures.stream().filter(t -> matches(t, type)).count();
		}

		private static boolean matches(Throwable t, Class<? extends Throwable> type) {
			for (Throwable current = t; current != null; current = current.getCause()) {
				if (type.isInstance(current)) {
					return true;
				}
			}
			return false;
		}
	}
}
