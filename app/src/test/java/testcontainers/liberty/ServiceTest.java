package testcontainers.liberty;

import static org.junit.Assert.assertEquals;

import static io.restassured.RestAssured.given;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

@Testcontainers
public class ServiceTest {

    // Latest liberty image
    static final DockerImageName libertyImage = DockerImageName.parse("open-liberty:23.0.0.4-full-java11-openj9");

    // Create container and copy application + server.xml
    @Container
    static final GenericContainer<?> liberty = new GenericContainer<>(libertyImage)
            .withExposedPorts(9080, 9443)
            .withCopyFileToContainer(MountableFile.forHostPath("build/libs/testapp.war"), "/config/dropins/testapp.war")
            .withCopyFileToContainer(MountableFile.forHostPath("build/resources/main/liberty/config/server.xml"), "/config/server.xml")
            .waitingFor(Wait.forLogMessage(".*CWWKZ0001I: Application .* started in .* seconds.*", 1))
            .withLogConsumer(new LogConsumer(ServiceTest.class, "liberty"));

    // Setup RestAssured to query our service
    static RequestSpecification requestSpecification;

    @BeforeAll
    static void getServiceURL() {
        String baseUri = "http://" + liberty.getHost() + ":" + liberty.getMappedPort(9080) + "/testapp/people";
        requestSpecification = new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .build();
    }

    @Test
    public void testAddPerson() {
        // Add new person to service
        given(requestSpecification)
            .header("Content-Type", "application/json")
            .queryParam("name", "bob")
            .queryParam("age", "24")
        .when()
            .post("/")
        .then()
            .statusCode(200);

        // Get and verify only one person exists
        List<Long> allIDs = given(requestSpecification)
            .accept("application/json")
        .when()
            .get("/")
        .then()
            .statusCode(200)
        .extract().body()
            .jsonPath().getList("id");

        assertEquals(1, allIDs.size());

        // Verify that person is 'bob'
        String actual = given(requestSpecification)
            .accept("application/json")
        .when()
            .get("/" + allIDs.get(0))
        .then()
            .statusCode(200)
        .extract().body()
            .jsonPath().getString("name");

        assertEquals("bob", actual);
    }
}
