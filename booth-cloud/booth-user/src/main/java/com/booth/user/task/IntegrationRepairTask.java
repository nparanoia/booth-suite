package com.booth.user.task;

import com.booth.user.service.IntegrationRepairService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntegrationRepairTask {

    private final IntegrationRepairService integrationRepairService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 90_000)
    public void refundClosedOrMissing() {
        integrationRepairService.refundClosedOrMissing();
    }
}
