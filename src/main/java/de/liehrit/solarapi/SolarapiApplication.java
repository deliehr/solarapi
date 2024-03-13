package de.liehrit.solarapi;

import jakarta.annotation.PostConstruct;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;

import java.util.Arrays;
import java.util.Properties;
import java.util.stream.StreamSupport;

@SpringBootApplication
public class SolarapiApplication {
	private static final Logger log = LoggerFactory.getLogger(SolarapiApplication.class);

	@Autowired
	AbstractEnvironment abstractEnvironment;

	public static void main(String[] args) {
		SpringApplication.run(SolarapiApplication.class, args);
	}

	@PostConstruct
	private void containerStarted() {
		MutablePropertySources propSrcs = abstractEnvironment.getPropertySources();
		val arr = StreamSupport.stream(propSrcs.spliterator(), false)
				.filter(ps -> ps instanceof EnumerablePropertySource)
				.map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
				.flatMap(Arrays::<String>stream)
				.sorted()
				.toArray();

		log.debug("");

		for(Object element:arr) {
			log.debug("{}", element);
		}
	}
}