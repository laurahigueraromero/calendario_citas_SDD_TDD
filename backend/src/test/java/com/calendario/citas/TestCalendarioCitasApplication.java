package com.calendario.citas;

import org.springframework.boot.SpringApplication;

public class TestCalendarioCitasApplication {

	public static void main(String[] args) {
		SpringApplication.from(CalendarioCitasApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
