package de.liehrit.solarapi.repositories;

import de.liehrit.solarapi.model.LogMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MyRepository extends MongoRepository<LogMessage, String> {}
