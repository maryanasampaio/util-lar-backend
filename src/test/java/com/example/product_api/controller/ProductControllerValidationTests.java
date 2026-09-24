package com.example.product_api.controller;

import com.example.product_api.dto.ProductDTO.ProductResponseDTO;
import com.example.product_api.exception.GlobalExceptionHandler;
import com.example.product_api.service.ProductService;
import com.example.product_api.service.StorageService;
import com.example.product_api.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ProductControllerValidationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private StorageService storageService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void shouldAcceptEmptyPayloadWhenCreatingProduct() throws Exception {
        when(jwtUtil.extractUsername("valid-token")).thenReturn("admin");
        when(jwtUtil.isAccessToken("valid-token")).thenReturn(true);
        when(jwtUtil.validateToken("valid-token", "admin")).thenReturn(true);
        when(productService.create(any())).thenReturn(new ProductResponseDTO(
                1L,
                "Produto sem nome",
                "produto-sem-nome",
                "",
                0L,
                null,
                null,
                "novo",
                "Sem categoria",
                null,
                0,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                1,
                null,
                null
        ));

        String body = "{}";

        mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Produto sem nome"))
                .andExpect(jsonPath("$.price").value(0))
                .andExpect(jsonPath("$.category").value("Sem categoria"));
    }

    @Test
    void shouldAcceptShortDescriptionWhenCreatingProduct() throws Exception {
        when(jwtUtil.extractUsername("valid-token")).thenReturn("admin");
        when(jwtUtil.isAccessToken("valid-token")).thenReturn(true);
        when(jwtUtil.validateToken("valid-token", "admin")).thenReturn(true);
        when(productService.create(any())).thenReturn(new ProductResponseDTO(
                1L,
                "Sofá",
                "sofa",
                "fdf",
                1000L,
                null,
                null,
                "novo",
                "Sofá",
                java.util.List.of("img1.jpg"),
                1,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                1,
                null,
                null
        ));

        String body = "{\n" +
                "  \"name\": \"Sofá\",\n" +
                "  \"description\": \"fdf\",\n" +
                "  \"price\": 1000,\n" +
                "  \"condition\": \"novo\",\n" +
                "  \"category\": \"Sofá\",\n" +
                "  \"images\": [\"img1.jpg\"],\n" +
                "  \"stock\": 1,\n" +
                "  \"featured\": false\n" +
                "}";

        mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("fdf"));
    }
}
