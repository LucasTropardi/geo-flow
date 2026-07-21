package br.com.lucastropardi.geoflow.notifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class GeoflowNotifierApplication {

	public static void main(String[] args) {
		SpringApplication.run(GeoflowNotifierApplication.class, args);
	}

}
