package com.laptophub.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.backend.dto.ProductCreateDTO;
import com.laptophub.backend.dto.ProductResponseDTO;
import com.laptophub.backend.model.Product;
import com.laptophub.backend.model.ProductImage;
import com.laptophub.backend.repository.CartItemRepository;
import com.laptophub.backend.repository.OrderItemRepository;
import com.laptophub.backend.repository.ProductRepository;
import com.laptophub.backend.repository.ProductImageRepository;
import com.laptophub.backend.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Tests de endpoints CRUD para Product
 * Ejecuta las pruebas en orden específico para mantener consistencia
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("null")
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    private static String productId; // Para compartir entre tests
    private static final String TEST_PRODUCT_NAME = "Laptop Dell XPS 15";
    private static final String TEST_BRAND = "Dell";

    /**
     * Limpia la base de datos una sola vez antes de todos los tests
     */
    @BeforeAll
    public static void setUpDatabase() {
        // La limpieza ocurre una sola vez al inicio
    }

    /**
     * TEST 1: Crear producto (POST /api/products)
     */
    @Test
    @Order(1)
    @SuppressWarnings("null")
    public void test1_CreateProduct() throws Exception {
        // Limpiar BD solo antes del primer test
        orderItemRepository.deleteAll();
        cartItemRepository.deleteAll();
        reviewRepository.deleteAll();
        productImageRepository.deleteAll();
        productRepository.deleteAll();
        
        System.out.println("\n=== TEST 1: Crear nuevo producto (POST /api/products) ===");
        
        ProductCreateDTO newProduct = ProductCreateDTO.builder()
                .nombre(TEST_PRODUCT_NAME)
                .descripcion("Laptop de alto rendimiento para profesionales")
                .precio(new BigDecimal("1299.99"))
                .stock(25)
                .marca(TEST_BRAND)
                .procesador("Intel Core i7-12700H")
                .ram(16)
                .almacenamiento(512)
                .pantalla("15.6 pulgadas FHD")
                .gpu("NVIDIA RTX 3050")
                .peso(new BigDecimal("1.86"))
                .build();

        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value(TEST_PRODUCT_NAME))
                .andExpect(jsonPath("$.marca").value(TEST_BRAND))
                .andExpect(jsonPath("$.precio").value(1299.99))
                .andExpect(jsonPath("$.stock").value(25))
                .andExpect(jsonPath("$.imagenes").isArray())
                .andExpect(jsonPath("$.resenas").isArray())
                .andReturn();

        // Guardar el ID para los siguientes tests
        String response = result.getResponse().getContentAsString();
        ProductResponseDTO createdProduct = objectMapper.readValue(response, ProductResponseDTO.class);
        productId = createdProduct.getId().toString();
        
        // Agregar imagen principal manualmente a la BD
        Product product = productRepository.findById(Long.parseLong(productId))
                .orElseThrow(() -> new RuntimeException("Product not found"));
        ProductImage mainImage = ProductImage.builder()
                .url("https://example.com/dell-xps-15.jpg")
                .product(product)
                .orden(0)
                .descripcion("Imagen principal")
                .build();
        productImageRepository.save(mainImage);
        
        System.out.println("✅ TEST 1 PASÓ: Producto creado con ID: " + productId + "\n");
    }

    /**
     * TEST 2: Buscar producto por ID (GET /api/products/{id})
     */
    @Test
    @Order(2)
    public void test2_FindProductById() throws Exception {
        System.out.println("\n=== TEST 2: Buscar producto por ID (GET /api/products/{id}) ===");
        
        mockMvc.perform(get("/api/products/" + productId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.nombre").value(TEST_PRODUCT_NAME))
                .andExpect(jsonPath("$.marca").value(TEST_BRAND));
        
        System.out.println("✅ TEST 2 PASÓ: Producto encontrado por ID\n");
    }

    /**
     * TEST 3: Listar todos los productos (GET /api/products) con paginación
     */
    @Test
    @Order(3)
    public void test3_FindAllProducts() throws Exception {
        System.out.println("\n=== TEST 3: Listar todos los productos (GET /api/products) ===");
        
        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists());
        
        System.out.println("✅ TEST 3 PASÓ: Lista paginada de productos obtenida\n");
    }

    /**
     * TEST 4: Buscar producto por nombre (GET /api/products/search?nombre=...) con paginación
     */
    @Test
    @Order(4)
    public void test4_SearchProductByName() throws Exception {
        System.out.println("\n=== TEST 4: Buscar producto por nombre (GET /api/products/search) ===");
        
        mockMvc.perform(get("/api/products/search")
                        .param("nombre", "Dell XPS")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].nombre").value(TEST_PRODUCT_NAME))
                .andExpect(jsonPath("$.totalElements").exists());
        
        System.out.println("✅ TEST 4 PASÓ: Producto encontrado por nombre (paginado)\n");
    }

    /**
     * TEST 5: Buscar productos por marca (GET /api/products/brand?marca=...) con paginación
     */
    @Test
    @Order(5)
    public void test5_FindProductsByBrand() throws Exception {
        System.out.println("\n=== TEST 5: Buscar productos por marca (GET /api/products/brand) ===");
        
        mockMvc.perform(get("/api/products/brand")
                        .param("marca", TEST_BRAND)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].marca").value(TEST_BRAND))
                .andExpect(jsonPath("$.totalElements").exists());
        
        System.out.println("✅ TEST 5 PASÓ: Productos encontrados por marca (paginados)\n");
    }

    /**
     * TEST 6: Actualizar producto (PUT /api/products/{id})
     */
    @Test
    @Order(6)
        @SuppressWarnings("null")
    public void test6_UpdateProduct() throws Exception {
        System.out.println("\n=== TEST 6: Actualizar producto (PUT /api/products/{id}) ===");
        
        Product updateData = Product.builder()
                .nombre("Laptop Dell XPS 15 (Actualizado)")
                .descripcion("Laptop de alto rendimiento - versión actualizada")
                .precio(new BigDecimal("1199.99"))
                .stock(30)
                .marca(TEST_BRAND)
                .procesador("Intel Core i7-12700H")
                .ram(32)
                .almacenamiento(1024)
                .pantalla("15.6 pulgadas FHD")
                .gpu("NVIDIA RTX 3050 Ti")
                .peso(new BigDecimal("1.86"))
                .build();

        mockMvc.perform(put("/api/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.nombre").value("Laptop Dell XPS 15 (Actualizado)"))
                .andExpect(jsonPath("$.precio").value(1199.99))
                .andExpect(jsonPath("$.ram").value(32))
                .andExpect(jsonPath("$.almacenamiento").value(1024));
        
        System.out.println("✅ TEST 6 PASÓ: Producto actualizado correctamente\n");
    }

    /**
     * TEST 7: Eliminar producto (DELETE /api/products/{id})
     */
    @Test
    @Order(7)
    public void test7_DeleteProduct() throws Exception {
        System.out.println("\n=== TEST 7: Eliminar producto (DELETE /api/products/{id}) ===");
        
        mockMvc.perform(delete("/api/products/" + productId))
                .andDo(print())
                .andExpect(status().isOk());
        
        System.out.println("✅ TEST 7 PASÓ: Producto eliminado correctamente\n");
    }

    /**
     * TEST 8: Crear producto final para verificación manual en BD
     */
    @Test
    @Order(8)
        @SuppressWarnings("null")
    public void test8_CreateFinalProductForVerification() throws Exception {
        System.out.println("\n=== TEST 8: Crear producto final para verificación manual ===");
        
        Product finalProduct = Product.builder()
                .nombre("Laptop HP Pavilion Gaming")
                .descripcion("Laptop gaming con excelente relación calidad-precio")
                .precio(new BigDecimal("899.99"))
                .stock(15)
                .marca("HP")
                .procesador("AMD Ryzen 7 5800H")
                .ram(16)
                .almacenamiento(512)
                .pantalla("15.6 pulgadas FHD 144Hz")
                .gpu("NVIDIA RTX 3060")
                .peso(new BigDecimal("2.23"))
                .build();

        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(finalProduct)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Laptop HP Pavilion Gaming"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Product createdProduct = objectMapper.readValue(response, Product.class);
        
        // Agregar 3 imágenes usando ProductImage
        ProductImage image1 = ProductImage.builder()
                .url("https://example.com/hp-pavilion-main.jpg")
                .orden(0)
                .descripcion("Vista principal")
                .product(createdProduct)
                .build();
        productImageRepository.save(image1);
        
        ProductImage image2 = ProductImage.builder()
                .url("https://example.com/hp-pavilion-side.jpg")
                .orden(1)
                .descripcion("Vista lateral")
                .product(createdProduct)
                .build();
        productImageRepository.save(image2);

        ProductImage image3 = ProductImage.builder()
                .url("https://example.com/hp-pavilion-ports.jpg")
                .orden(2)
                .descripcion("Puertos laterales")
                .product(createdProduct)
                .build();
        productImageRepository.save(image3);
        
        System.out.println("✅ TEST 8 PASÓ: Producto final creado con ID: " + createdProduct.getId());
                System.out.println("✅ 3 imágenes agregadas a product_images");
        System.out.println("📋 Verifica en tu gestor de BD:");
        System.out.println("   - Producto: Laptop HP Pavilion Gaming");
                System.out.println("   - 3 imágenes en product_images (orden 0-2)");
        System.out.println("   - Campo imagen_url debe estar NULL\n");
    }
}
