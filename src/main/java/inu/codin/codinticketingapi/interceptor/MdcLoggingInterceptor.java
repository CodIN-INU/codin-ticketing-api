package inu.codin.codinticketingapi.interceptor;

import inu.codin.codinticketingapi.common.constant.LogConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class MdcLoggingInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestTxId = request.getHeader(LogConstant.HEADER_TX_ID);
        String txId;

        if (requestTxId != null && !requestTxId.isEmpty()) {
            txId = requestTxId;
        } else {
            txId = UUID.randomUUID().toString().substring(0, 8);
        }

        MDC.put(LogConstant.MDC_TX_ID, txId);

        response.setHeader(LogConstant.HEADER_TX_ID, txId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        MDC.clear();
    }
}
