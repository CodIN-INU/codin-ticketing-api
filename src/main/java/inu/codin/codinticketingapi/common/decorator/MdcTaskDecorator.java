package inu.codin.codinticketingapi.common.decorator;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public @NonNull Runnable decorate(@NonNull Runnable runnable) {
        Map<String, String> callerContextMap = MDC.getCopyOfContextMap();

        return () -> {
            if (callerContextMap != null) {
                MDC.setContextMap(callerContextMap);
            }

            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
