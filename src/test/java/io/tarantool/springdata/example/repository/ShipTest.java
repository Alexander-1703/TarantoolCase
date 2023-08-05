package io.tarantool.springdata.example.repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.TarantoolContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.apache.commons.lang.NotImplementedException;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.springdata.example.AbstractBaseIntegrationTest;
import io.tarantool.springdata.example.model.Ship;

@Testcontainers
@TestMethodOrder(MethodOrderer.Random.class)
public class ShipTest extends AbstractBaseIntegrationTest {

	final static Logger log = LoggerFactory.getLogger(AbstractBaseIntegrationTest.class);

	@Container
	private static final TarantoolContainer tt = new TarantoolContainer().withLogConsumer(new Slf4jLogConsumer(log));

	private static Ship tuple;

	private final ShipsRepository repository;
	private final TarantoolClient client;

	@Autowired
	public ShipTest(ShipsRepository repository, TarantoolClient client) {
		this.repository = repository;
		this.client = client;
	}

	@BeforeAll
	static void beforeAll() {
		tuple = Ship.builder()
			.id(1)
			.name("Lesnoe")
			.crew(800)
			.gunsCount(90)
			.createdAt(LocalDateTime.parse("1999-10-09T19:05:05.999999997").toInstant(ZoneOffset.UTC))
			.build();
	}

	@BeforeEach
	void beforeEach() {
		repository.deleteAll();
	}

	@AfterEach
	void afterEach() {
		repository.deleteAll();
	}

	@Test
	public void testEval() throws ExecutionException, InterruptedException {
		assertEquals(tuple.getCreatedAt(),
					 client.eval("return ...",
								 Collections.singletonList(tuple.getCreatedAt())).get().get(0));
	}

	@Test
	public void testSaveAndFind() {
		throw new NotImplementedException();
	}
}
