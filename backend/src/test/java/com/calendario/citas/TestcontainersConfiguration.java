package com.calendario.citas;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		// Versión fija para reproducibilidad: el control de concurrencia del PRD
		// depende del comportamiento de bloqueo de InnoDB (SELECT ... FOR UPDATE).
		return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
	}

}
