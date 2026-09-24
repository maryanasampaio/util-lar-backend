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
	"spring.flyway.enabled=false",
	"storage.s3.url=http://localhost:3900",
	"storage.s3.bucket=util-lar-test",
	"storage.s3.key-id=test-key",
	"storage.s3.secret-key=test-secret",
	"storage.s3.region=garage",
	"app.api-base-url=http://localhost:8080"
})
class ProductApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
