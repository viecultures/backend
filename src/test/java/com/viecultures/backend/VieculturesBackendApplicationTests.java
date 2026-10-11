package com.viecultures.backend;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VieculturesBackendApplicationTests {

    @BeforeAll
    static void init() {
        VieculturesBackendApplication.loadDotEnv();
    }

    @Test
    void contextLoads() {
    }

}
