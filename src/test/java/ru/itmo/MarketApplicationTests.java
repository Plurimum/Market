package ru.itmo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.itmo.controller.ProductController;
import ru.itmo.controller.UserController;
import ru.itmo.model.Product;
import ru.itmo.model.User;
import ru.itmo.repository.ProductRepository;
import ru.itmo.repository.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@WebFluxTest(value = {ProductController.class, UserController.class})
public class MarketApplicationTests {
    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ProductRepository productRepository;

    private static final String USER_NAME = "Maxim";
    private static final String DOLLAR = "dollar";
    private static final String RUBLE = "ruble";

    private static final User USER = new User(null, USER_NAME, RUBLE);
    private static final Product PRODUCT_RUBLE = new Product(null, "milk", 70, RUBLE);
    private static final Product PRODUCT_DOLLAR = new Product(null, "milk", 1, DOLLAR);

    private User user;

    @BeforeEach
    void prepare() throws JsonProcessingException {
        when(userRepository.save(USER))
                .thenReturn(Mono.just(new User(getUuid(), USER.name(), USER.currency())));

        this.user = objectMapper.readValue(
                getPostResult("/save/user", USER),
                User.class
        );

        when(userRepository.findById(user.id()))
                .thenReturn(Mono.just(user));
    }

    @Test
    void testUserAndProductCurrency() throws Exception {
        Product product = addProduct(PRODUCT_RUBLE);

        when(productRepository.findByCurrency(user.currency()))
                .thenReturn(Flux.just(product));

        List<Product> result = objectMapper.readValue(
                getGetResult("/get/products/" + user.id()),
                new TypeReference<>() {
                }
        );

        assertEquals(List.of(product), result);
    }

    @Test
    void testDifferentUserAndProductCurrency() throws JsonProcessingException {
        addProduct(PRODUCT_DOLLAR);

        when(productRepository.findByCurrency(user.currency()))
                .thenReturn(Flux.empty());

        List<Product> result = objectMapper.readValue(
                getGetResult("/get/products/" + user.id()),
                new TypeReference<>() {
                }
        );

        assertEquals(Collections.emptyList(), result);
    }

    private Product addProduct(Product product) throws JsonProcessingException {
        when(productRepository.save(PRODUCT_RUBLE))
                .thenReturn(
                        Mono.just(
                                new Product(
                                        getUuid(),
                                        PRODUCT_RUBLE.name(),
                                        PRODUCT_RUBLE.price(),
                                        PRODUCT_RUBLE.currency()
                                )
                        )
                );

        return objectMapper.readValue(
                getPostResult("/save/product", PRODUCT_RUBLE),
                Product.class
        );
    }

    private String getUuid() {
        return UUID.randomUUID().toString();
    }

    private <T> String getPostResult(String path, T body) {
        return webClient.post()
                .uri(path)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();
    }

    private String getGetResult(String path) {
        return webClient.get()
                .uri(path)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();
    }
}
