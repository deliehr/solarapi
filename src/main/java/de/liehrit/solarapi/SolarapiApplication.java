package de.liehrit.solarapi;

import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import lombok.val;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
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

import java.io.FileReader;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.StreamSupport;

@SpringBootApplication
public class SolarapiApplication {
	public static void main(String[] args) {
		SpringApplication.run(SolarapiApplication.class, args);
	}

	public static Model getPomModel() throws Exception {
		MavenXpp3Reader reader = new MavenXpp3Reader();
		return reader.read(new FileReader("pom.xml"));
	}
}