package com.example.product_api;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.example.product_api.service.StorageService;

@SpringBootTest
@TestPropertySource(properties = {
	"spring.datasource.url=jdbc:h2:mem:productdb-nostorage;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.jpa.hibernate.ddl-auto=none",
	"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
	"spring.jpa.show-sql=false",
	"spring.flyway.enabled=false",
	"storage.s3.url=",
	"storage.s3.bucket=",
	"storage.s3.key-id=",
	"storage.s3.secret-key="
})
class ProductApiApplicationWithoutStorageTests {

	@Autowired
	private StorageService storageService;

	@Test
	void contextLoadsWithoutStorageConfiguration() {
		assertFalse(storageService.isConfigured());
	}

}
