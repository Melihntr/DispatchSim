package com.dispatchsim;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DispatchsimApplicationTests {

    @Test
    void contextLoads() {
        // Spring Boot'un ayağa kalktığını test eder
    }

    @Test
    void main() {
        // Main metodunu sahte argümanlarla tetikleyip %100 kapsama sağlar
        DispatchsimApplication.main(new String[] {});
    }
}