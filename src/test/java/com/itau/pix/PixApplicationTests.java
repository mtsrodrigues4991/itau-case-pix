package com.itau.pix;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Sobe o contexto completo sobre H2 (profile "test"), sem depender de Docker.
 * Garante que entidades, repositorios, validators e o service amarram de verdade.
 */
@SpringBootTest
@ActiveProfiles("test")
class PixApplicationTests {

    @Test
    void contextLoads() {
    }

}
