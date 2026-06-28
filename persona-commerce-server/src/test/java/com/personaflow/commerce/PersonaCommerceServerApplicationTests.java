package com.personaflow.commerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:mysql://localhost:3306/${MYSQL_DATABASE:persona_flow_commerce}",
		"spring.datasource.username=${MYSQL_USER:persona_flow}",
		"spring.datasource.password=${MYSQL_PASSWORD:123456}"
})
class PersonaCommerceServerApplicationTests {

	@Test
	void contextLoads() {
	}

}
