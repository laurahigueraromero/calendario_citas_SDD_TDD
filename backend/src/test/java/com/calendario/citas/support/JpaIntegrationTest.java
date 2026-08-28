package com.calendario.citas.support;

import com.calendario.citas.TestcontainersConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Clase base para tests de <em>slice</em> de persistencia: carga solo los
 * componentes JPA (entidades, repositorios) contra el MySQL real de
 * Testcontainers, y ejecuta las migraciones de Flyway.
 *
 * <p>Cada método de test corre en una transacción que se revierte al terminar,
 * así que los tests no se contaminan entre sí.
 *
 * <p>Convención de nombres: {@code *IT}, ejecutados por failsafe en {@code verify}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class JpaIntegrationTest {
}
