package com.example.product_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
	"spring.datasource.url=jdbc:h2:mem:productdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.jpa.hibernate.ddl-auto=none",
	"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
	"spring.jpa.show-sql=false",
	"spring.flyway.enabled=false"
})
class ProductApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
