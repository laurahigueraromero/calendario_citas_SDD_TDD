package com.calendario.citas.support;

import com.calendario.citas.TestcontainersConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Clase base para tests de integración: arranca el contexto de Spring completo
 * contra una instancia real de MySQL 8.4 gestionada con Testcontainers
 * (ver {@link TestcontainersConfiguration}).
 *
 * <p>Todas las subclases que no añadan configuración propia comparten el mismo
 * contexto de Spring cacheado y, por tanto, el mismo contenedor de MySQL: el
 * contenedor se levanta una sola vez para toda la suite.
 *
 * <p>Convención de nombres: los tests de integración terminan en {@code *IT} y
 * los ejecuta el plugin failsafe en la fase {@code verify}.
 *
 * <p>Activa el perfil {@code test} ({@code application-test.yml}).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {
}
