package com.codingagent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.ai.mistralai.api-key=test-key"
})
class CodingAgentApplicationTests {

    @Test
    void contextLoads() {
    }

}
