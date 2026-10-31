package io.github.messaoudidev.terminbuchung;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(locations = "classpath:test.properties")
class TerminbuchungApplicationTests {

	@Test
	void contextLoads() {
	}

}
