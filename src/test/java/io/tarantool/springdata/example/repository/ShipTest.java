package io.tarantool.springdata.example.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.testcontainers.containers.TarantoolContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.springdata.example.AbstractBaseIntegrationTest;
import io.tarantool.springdata.example.model.Ship;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestMethodOrder(MethodOrderer.Random.class)
public class ShipTest extends AbstractBaseIntegrationTest {

    private static final int NON_EXISTENT_ID = -1;
    private static final int INVALID_UNSIGNED_INT = -1;
    private static final String ANOTHER_SHIP_NAME = "Titanic";

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
        Ship savedShip = repository.save(tuple);
        assertNotNull(savedShip);

        Optional<Ship> foundShipOptional = repository.findById(savedShip.getId());
        assertTrue(foundShipOptional.isPresent());

        Ship foundShip = foundShipOptional.get();
        assertAll("Checking ship properties",
                () -> assertEquals(tuple.getName(), foundShip.getName()),
                () -> assertEquals(tuple.getCrew(), foundShip.getCrew()),
                () -> assertEquals(tuple.getGunsCount(), foundShip.getGunsCount()),
                () -> assertEquals(tuple.getCreatedAt(), foundShip.getCreatedAt())
        );
    }

    @Test
    public void testDeleteShip() {
        Ship savedShip = repository.save(tuple);

        repository.deleteById(savedShip.getId());

        Optional<Ship> foundShipOptional = repository.findById(savedShip.getId());
        assertFalse(foundShipOptional.isPresent());
    }

    @Test
    public void testUpdateShip() {
        Ship savedShip = repository.save(tuple);

        String updateName = ANOTHER_SHIP_NAME;
        savedShip.setName(updateName);
        repository.save(savedShip);

        assertEquals(updateName, savedShip.getName());
    }

    @Test
    public void testSaveInvalidName() {
        Ship shipWithInvalidName = tuple.toBuilder().name(null).build();

        assertThrows(DataRetrievalFailureException.class, () -> repository.save(shipWithInvalidName));
    }

    @Test
    public void testSaveInvalidGuns() {
        Ship shipWithInvalidGuns = tuple.toBuilder().gunsCount(INVALID_UNSIGNED_INT).build();

        assertThrows(DataRetrievalFailureException.class, () -> repository.save(shipWithInvalidGuns));
    }

    @Test
    public void testSaveInvalidCrew() {
        Ship shipWithInvalidCrew = tuple.toBuilder().crew(INVALID_UNSIGNED_INT).build();

        assertThrows(DataRetrievalFailureException.class, () -> repository.save(shipWithInvalidCrew));
    }

    @Test
    public void testSaveNullDate() {
        Ship shipWithNullDate = tuple.toBuilder().createdAt(null).build();

        assertThrows(DataRetrievalFailureException.class, () -> repository.save(shipWithNullDate));
    }

    @Test
    @Disabled
    // Я подумал, что запись с кораблем с датой постройки большей, тем текущая, будет некорректной.
    // Но так как нет бизнес правил, то непонятно, являются ли такие записи корректными
    // Тест не будет проходить, так как нет таких ограничений на дату
    public void testSaveFutureCreatedAt() {
        Instant futureInstantDate = LocalDateTime.now().plusDays(1).atZone(ZoneOffset.UTC).toInstant();
        Ship shipWithFutureCreatedAt = tuple.toBuilder().createdAt(futureInstantDate).build();

        assertThrows(DataRetrievalFailureException.class, () -> repository.save(shipWithFutureCreatedAt));
    }

    @Test
    public void testFindByNonExistentId() {
        Optional<Ship> optionalEmpty = repository.findById(NON_EXISTENT_ID);

        assertFalse(optionalEmpty.isPresent());
    }

    @Test
    public void testDeleteNonExistentShip() {
        repository.deleteById(NON_EXISTENT_ID);
    }

    @Test
    public void testFindAllShips() {
        repository.save(tuple);
        repository.save(
                Ship.builder()
                        .id(NON_EXISTENT_ID)
                        .name(ANOTHER_SHIP_NAME)
                        .createdAt(Instant.now())
                        .crew(0)
                        .gunsCount(0)
                        .build()
        );

        Iterable<Ship> allShips = repository.findAll();
        int count = 0;
        int expectedCount = 2;
        for (Ship ignored : allShips) {
            count++;
        }

        assertEquals(expectedCount, count);
    }

    @Test
    @Disabled
    // @RepeatedTest(10)
    // Поскольку Tarantool по умолчанию не обеспечивает механизмов для обработки многопоточности,
    // такой тест не будет проходить
    public void testConcurrentUpdateShip() throws ExecutionException, InterruptedException {
        // Given
        Ship savedShip = repository.save(tuple);
        int gunsCountBefore = savedShip.getGunsCount();
        int threadsCount = 5;

        ExecutorService executorService = Executors.newFixedThreadPool(threadsCount);
        Callable<Void> updateTask = () -> {
            Ship shipToUpdate = repository.findById(savedShip.getId()).orElseThrow(IllegalStateException::new);
            shipToUpdate.setGunsCount(shipToUpdate.getGunsCount() + 1);
            repository.save(shipToUpdate);
            return null;
        };

        // When
        List<Future<Void>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threadsCount; i++) {
                Future<Void> future = executorService.submit(updateTask);
                futures.add(future);
            }

            for (Future<Void> future : futures) {
                future.get();
            }
        } finally {
            executorService.shutdown();
        }

        // Then
        Ship updatedShip = repository.findById(savedShip.getId()).orElseThrow(IllegalStateException::new);
        assertEquals(gunsCountBefore + threadsCount, updatedShip.getGunsCount());
    }
}
