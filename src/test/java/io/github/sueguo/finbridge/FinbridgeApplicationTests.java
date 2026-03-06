package io.github.sueguo.finbridge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@SpringBootTest
@ActiveProfiles("test")
class FinbridgeApplicationTests {

    @MockBean
    private S3Client s3Client;

    @MockBean
    private S3Presigner s3Presigner;
    @Test
    void contextLoads() {
    }

}
