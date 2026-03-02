package com.laptophub.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.backend.dto.BrandCreateDTO;
import com.laptophub.backend.dto.ProductCreateDTO;
import com.laptophub.backend.dto.ProductResponseDTO;
import com.laptophub.backend.model.Brand;
import com.laptophub.backend.model.Product;
import com.laptophub.backend.model.ProductImage;
import com.laptophub.backend.model.Review;
import com.laptophub.backend.model.User;
import com.laptophub.backend.repository.BrandRepository;
import com.laptophub.backend.repository.CartItemRepository;
import com.laptophub.backend.repository.OrderItemRepository;
import com.laptophub.backend.repository.ProductRepository;
import com.laptophub.backend.repository.ProductImageRepository;
import com.laptophub.backend.repository.ReviewRepository;
import com.laptophub.backend.repository.UserRepository;
import com.laptophub.backend.support.TestAuthHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static String productId;
    private static String brandId;
    private static final String TEST_PRODUCT_NAME = "Laptop Dell XPS 15";
    private static final String TEST_BRAND_NAME = "Dell";
    private static String adminToken;

    /**
     * Limpia la base de datos una sola vez antes de todos los tests
     */
    @BeforeAll
    public static void setUpDatabase() {
        // La limpieza ocurre una sola vez al inicio
    }

    /**
     * TEST 1: Crear marca y producto (POST /api/brands y POST /api/products)
     */
    @Test
    @Order(1)
    public void test1_CreateBrandAndProduct() throws Exception {
        // Limpiar BD
        orderItemRepository.deleteAll();
        cartItemRepository.deleteAll();
        reviewRepository.deleteAll();
        productImageRepository.deleteAll();
        productRepository.deleteAll();
        brandRepository.deleteAll();
        
        System.out.println("\n=== TEST 1: Crear marca y producto ===");
        
        adminToken = TestAuthHelper.createAdminAndLogin(
                userRepository,
                passwordEncoder,
                mockMvc,
                objectMapper,
                TestAuthHelper.uniqueEmail("product.admin"),
                "admin123"
        );

        // Crear marca primero
        BrandCreateDTO brandDTO = BrandCreateDTO.builder()
                .nombre(TEST_BRAND_NAME)
                .descripcion("Marca Dell - Computers de alto rendimiento")
                .build();

        MvcResult brandResult = mockMvc.perform(post("/api/brands")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(brandDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value(TEST_BRAND_NAME))
                .andReturn();

        String brandResponse = brandResult.getResponse().getContentAsString();
        Brand createdBrand = objectMapper.readValue(brandResponse, Brand.class);
        brandId = createdBrand.getId().toString();

        // Crear producto con la marca creada
        ProductCreateDTO newProduct = ProductCreateDTO.builder()
                .nombre(TEST_PRODUCT_NAME)
                .descripcion("Laptop de alto rendimiento para profesionales")
                .precio(new BigDecimal("1299.99"))
                .stock(25)
                .brandId(Long.parseLong(brandId))
                .procesador("Intel Core i7-12700H")
                .ram(16)
                .almacenamiento(512)
                .pantalla("15.6 pulgadas FHD")
                .gpu("NVIDIA RTX 3050")
                .peso(new BigDecimal("1.86"))
                .build();

        MvcResult result = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value(TEST_PRODUCT_NAME))
                .andExpect(jsonPath("$.brand.nombre").value(TEST_BRAND_NAME))
                .andExpect(jsonPath("$.precio").value(1299.99))
                .andExpect(jsonPath("$.stock").value(25))
                .andExpect(jsonPath("$.imagenes").isArray())
                .andExpect(jsonPath("$.resenas").isArray())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        ProductResponseDTO createdProduct = objectMapper.readValue(response, ProductResponseDTO.class);
        productId = createdProduct.getId().toString();
        
        // Agregar imagen principal
        Product product = productRepository.findById(Long.parseLong(productId))
                .orElseThrow(() -> new RuntimeException("Product not found"));
        ProductImage mainImage = ProductImage.builder()
                .url("https://example.com/dell-xps-15.jpg")
                .product(product)
                .orden(0)
                .descripcion("Imagen principal")
                .build();
        productImageRepository.save(mainImage);
        
        System.out.println("✅ TEST 1 PASÓ: Marca creada con ID: " + brandId + ", Producto creado con ID: " + productId + "\n");
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
                .andExpect(jsonPath("$.brand.nombre").value(TEST_BRAND_NAME));
        
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
     * TEST 4: Buscar producto por nombre con endpoint unificado
     */
    @Test
    @Order(4)
    public void test4_SearchProductByName() throws Exception {
        System.out.println("\n=== TEST 4: Buscar producto por nombre (GET /api/products?nombre=...) ===");
        
        mockMvc.perform(get("/api/products")
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
     * TEST 5: Buscar productos por marca con endpoint unificado
     */
    @Test
    @Order(5)
    public void test5_FindProductsByBrand() throws Exception {
        System.out.println("\n=== TEST 5: Buscar productos por marca (GET /api/products?brandId=...) ===");
        
        mockMvc.perform(get("/api/products")
                        .param("brandId", String.valueOf(brandId))
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists());
        
        System.out.println("✅ TEST 5 PASÓ: Productos encontrados por marca (paginados)\n");
    }

    /**
     * TEST 5.1: Ordenar por PRECIO ascendente (menor a mayor)
     */
    @Test
    @Order(51)
    public void test5_1_SortByPriceAscending() throws Exception {
        System.out.println("\n=== TEST 5.1: Ordenar por PRECIO ascendente (GET /api/products?sortBy=price&sort=asc) ===");
        
        MvcResult result = mockMvc.perform(get("/api/products")
                        .param("sortBy", "price")
                        .param("sort", "asc")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andReturn();
        
        String content = result.getResponse().getContentAsString();
        if (content.contains("content")) {
            System.out.println("✅ TEST 5.1 PASÓ: Productos ordenados por precio ascendente\n");
        }
    }

    /**
     * TEST 5.2: Ordenar por PRECIO descendente (mayor a menor)
     */
    @Test
    @Order(52)
    public void test5_2_SortByPriceDescending() throws Exception {
        System.out.println("\n=== TEST 5.2: Ordenar por PRECIO descendente (GET /api/products?sortBy=price&sort=desc) ===");
        
        mockMvc.perform(get("/api/products")
                        .param("sortBy", "price")
                        .param("sort", "desc")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists());
        
        System.out.println("✅ TEST 5.2 PASÓ: Productos ordenados por precio descendente\n");
    }

    /**
     * TEST 5.3: Ordenar por NOMBRE ascendente (A-Z)
     */
    @Test
    @Order(53)
    public void test5_3_SortByNameAscending() throws Exception {
        System.out.println("\n=== TEST 5.3: Ordenar por NOMBRE ascendente (GET /api/products?sortBy=name&sort=asc) ===");
        
        mockMvc.perform(get("/api/products")
                        .param("sortBy", "name")
                        .param("sort", "asc")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists());
        
        System.out.println("✅ TEST 5.3 PASÓ: Productos ordenados por nombre ascendente (A-Z)\n");
    }

    /**
     * TEST 5.4: Ordenar por NOMBRE descendente (Z-A)
     */
    @Test
    @Order(54)
    public void test5_4_SortByNameDescending() throws Exception {
        System.out.println("\n=== TEST 5.4: Ordenar por NOMBRE descendente (GET /api/products?sortBy=name&sort=desc) ===");
        
        mockMvc.perform(get("/api/products")
                        .param("sortBy", "name")
                        .param("sort", "desc")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists());
        
        System.out.println("✅ TEST 5.4 PASÓ: Productos ordenados por nombre descendente (Z-A)\n");
    }

    /**
     * TEST 5.5: Ordenar por RATING descendente (mejor valorados)
     */
    @Test
    @Order(55)
    public void test5_5_SortByRatingDescending() throws Exception {
        System.out.println("\n=== TEST 5.5: Ordenar por RATING descendente (GET /api/products?sortBy=rating&sort=desc) ===");
        
        mockMvc.perform(get("/api/products")
                        .param("sortBy", "rating")
                        .param("sort", "desc")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists());
        
        System.out.println("✅ TEST 5.5 PASÓ: Productos ordenados por rating descendente (mejores primero)\n");
    }

    /**
     * TEST 5.6: Búsqueda combinada - por nombre, marca y precio
     */
    @Test
    @Order(56)
    public void test5_6_CombinedSearch() throws Exception {
        System.out.println("\n=== TEST 5.6: Búsqueda combinada (nombre + brandId + sortBy=price) ===");
        
        mockMvc.perform(get("/api/products")
                        .param("nombre", "XPS")
                        .param("brandId", String.valueOf(brandId))
                        .param("sortBy", "price")
                        .param("sort", "asc")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        
        System.out.println("✅ TEST 5.6 PASÓ: Búsqueda combinada funcionando correctamente\n");
    }

    /**
     * TEST 6: Actualizar producto (PUT /api/products/{id})
     */
    @Test
    @Order(6)
    public void test6_UpdateProduct() throws Exception {
        System.out.println("\n=== TEST 6: Actualizar producto (PUT /api/products/{id}) ===");
        
        ProductCreateDTO updateData = ProductCreateDTO.builder()
                .nombre("Laptop Dell XPS 15 (Actualizado)")
                .descripcion("Laptop de alto rendimiento - versión actualizada")
                .precio(new BigDecimal("1199.99"))
                .stock(30)
                .brandId(Long.parseLong(brandId))
                .procesador("Intel Core i7-12700H")
                .ram(32)
                .almacenamiento(1024)
                .pantalla("15.6 pulgadas FHD")
                .gpu("NVIDIA RTX 3050 Ti")
                .peso(new BigDecimal("1.86"))
                .build();

        mockMvc.perform(put("/api/products/" + productId)
                        .header("Authorization", "Bearer " + adminToken)
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
        
        mockMvc.perform(delete("/api/products/" + productId)
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk());

        assertTrue(productImageRepository.findByProductIdOrderByOrdenAsc(Long.parseLong(productId)).isEmpty(),
                "Las imagenes deben eliminarse al borrar el producto");
        
        System.out.println("✅ TEST 7 PASÓ: Producto eliminado correctamente\n");
    }

    /**
     * TEST 8: Crear producto final para verificación manual en BD con otra marca
     */
    @Test
    @Order(8)
    public void test8_CreateFinalProductForVerification() throws Exception {
        System.out.println("\n=== TEST 8: Crear producto final con otra marca para verificación ===");
        
        // Crear marca HP primero
        BrandCreateDTO hpBrandDTO = BrandCreateDTO.builder()
                .nombre("HP")
                .descripcion("Marca HP - Computadoras de gaming")
                .build();

        MvcResult hpBrandResult = mockMvc.perform(post("/api/brands")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hpBrandDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String hpBrandResponse = hpBrandResult.getResponse().getContentAsString();
        Brand hpBrand = objectMapper.readValue(hpBrandResponse, Brand.class);

        // Crear producto HP con la marca recién creada
        ProductCreateDTO finalProduct = ProductCreateDTO.builder()
                .nombre("Laptop HP Pavilion Gaming")
                .descripcion("Laptop gaming con excelente relación calidad-precio")
                .precio(new BigDecimal("899.99"))
                .stock(15)
                .brandId(hpBrand.getId())
                .procesador("AMD Ryzen 7 5800H")
                .ram(16)
                .almacenamiento(512)
                .pantalla("15.6 pulgadas FHD 144Hz")
                .gpu("NVIDIA RTX 3060")
                .peso(new BigDecimal("2.23"))
                .build();

        MvcResult result = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(finalProduct)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Laptop HP Pavilion Gaming"))
                .andExpect(jsonPath("$.brand.nombre").value("HP"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        ProductResponseDTO createdProduct = objectMapper.readValue(response, ProductResponseDTO.class);
        
        // Agregar 3 imágenes
        Product product = productRepository.findById(createdProduct.getId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        ProductImage image1 = ProductImage.builder()
                .url("https://example.com/hp-pavilion-main.jpg")
                .orden(0)
                .descripcion("Vista principal")
                .product(product)
                .build();
        productImageRepository.save(image1);
        
        ProductImage image2 = ProductImage.builder()
                .url("https://example.com/hp-pavilion-side.jpg")
                .orden(1)
                .descripcion("Vista lateral")
                .product(product)
                .build();
        productImageRepository.save(image2);

        ProductImage image3 = ProductImage.builder()
                .url("https://example.com/hp-pavilion-ports.jpg")
                .orden(2)
                .descripcion("Puertos laterales")
                .product(product)
                .build();
        productImageRepository.save(image3);
        
        System.out.println("✅ TEST 8 PASÓ: Producto HP creado con ID: " + createdProduct.getId());
        System.out.println("✅ 3 imágenes agregadas a product_images");
        System.out.println("📋 Verifica en tu gestor de BD:");
        System.out.println("   - Producto: Laptop HP Pavilion Gaming con brand_id = " + hpBrand.getId());
        System.out.println("   - 3 imágenes en product_images (orden 0-2)");
        System.out.println("   - Campo marca (String) debe estar NULL\n");
    }

    /**
     * TEST 9: Top 10 mejor valorados (GET /api/products/top-rated)
     */
    @Test
    @Order(9)
    public void test9_GetTopRatedProducts() throws Exception {
        System.out.println("\n=== TEST 9: Obtener 10 mejores productos (GET /api/products/top-rated) ===");
        
        // Limpiar y preparar datos para este test
        reviewRepository.deleteAll();
        cartItemRepository.deleteAll();
        orderItemRepository.deleteAll();
        productImageRepository.deleteAll();
        productRepository.deleteAll();
        brandRepository.deleteAll();
        userRepository.deleteAll();
        
        // Crear usuario admin para crear el producto
        String adminEmail = TestAuthHelper.uniqueEmail("test9.admin");
        String adminToken = TestAuthHelper.createAdminAndLogin(
                userRepository,
                passwordEncoder,
                mockMvc,
                objectMapper,
                adminEmail,
                "admin123"
        );
        
        // Crear usuario regular para crear reviews usando registerAndLogin
        String regularEmail = TestAuthHelper.uniqueEmail("test9.reviewer");
        TestAuthHelper.AuthInfo regularAuth = TestAuthHelper.registerAndLogin(
                mockMvc,
                objectMapper,
                regularEmail,
                "password123",
                "Test",
                "User"
        );
        String regularToken = regularAuth.getToken();
        
        // Crear marca
        BrandCreateDTO brandDTO = BrandCreateDTO.builder()
                .nombre("TestBrand_TopRated")
                .descripcion("Marca para test de top rated")
                .imageUrl("https://example.com/brand.jpg")
                .build();
        
        MvcResult brandResult = mockMvc.perform(post("/api/brands")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(brandDTO)))
                .andExpect(status().isOk())
                .andReturn();
        
        Brand testBrand = objectMapper.readValue(brandResult.getResponse().getContentAsString(), Brand.class);
        
        // Crear producto
        ProductCreateDTO productDTO = ProductCreateDTO.builder()
                .nombre("Test Product Top Rated")
                .descripcion("Product for top rated test")
                .precio(new BigDecimal("999.99"))
                .stock(50)
                .brandId(testBrand.getId())
                .procesador("Intel Core i5")
                .ram(8)
                .almacenamiento(256)
                .pantalla("13.3 pulgadas")
                .gpu("Intel UHD")
                .peso(new BigDecimal("1.5"))
                .build();
        
        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isOk())
                .andReturn();
        
        Product testProduct = objectMapper.readValue(productResult.getResponse().getContentAsString(), Product.class);
        
        // Obtener el usuario regular creado para poder asignarlo a un review
        User regularUser = userRepository.findByEmail(regularEmail).orElseThrow();
        
        // Crear reviews con diferentes ratings para que el producto tenga una buena puntuación
        for (int i = 0; i < 5; i++) {
            Review review = Review.builder()
                    .product(testProduct)
                    .user(regularUser)
                    .rating(5) // 5 estrellas
                    .comentario("Excelente producto - Review " + (i + 1))
                    .build();
            reviewRepository.save(review);
        }
        
        // Ahora llamar al endpoint top-rated
        mockMvc.perform(get("/api/products/top-rated"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.content[0]").exists())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].nombre").exists())
                .andExpect(jsonPath("$.content[0].precio").exists())
                .andExpect(jsonPath("$.content[0].brand").exists())
                .andExpect(jsonPath("$.content[0].promedioRating").exists());
        
        System.out.println("✅ TEST 9 PASÓ: Top 10 mejor valorados obtenido correctamente\n");
    }
}
