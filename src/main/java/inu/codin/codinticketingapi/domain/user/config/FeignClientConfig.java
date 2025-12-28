package inu.codin.codinticketingapi.domain.user.config;

import feign.RequestInterceptor;
import inu.codin.codinticketingapi.common.constant.LogConstant;
import inu.codin.codinticketingapi.security.util.SecurityUtil;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            String jwtToken = getJwtToken();
            String txId = MDC.get(LogConstant.MDC_TX_ID);

            if (jwtToken != null && !jwtToken.isEmpty()) {
                requestTemplate.header("Authorization", "Bearer " + jwtToken);
            }

            if (txId != null) {
                requestTemplate.header(LogConstant.HEADER_TX_ID, txId);
            }
        };
    }

    private String getJwtToken() {
        return SecurityUtil.getUserToken();
    }
}
