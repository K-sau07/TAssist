package com.tassist.infrastructure.web.quota;

import com.tassist.application.quota.QuotaService;
import com.tassist.domain.error.Unauthenticated;
import com.tassist.domain.model.QuotaUsage;
import com.tassist.domain.vo.UserId;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** §16.2 GET /api/quota — current period usage vs limits (header banner + settings page). */
@RestController
@RequestMapping("/api/quota")
public class QuotaController {

    private final QuotaService quota;

    public QuotaController(QuotaService quota) { this.quota = quota; }

    @GetMapping
    public QuotaResponse get(Authentication auth) {
        UserId user = principal(auth);
        QuotaUsage u = quota.getCurrent(user);
        return new QuotaResponse(
            u.period().toString(),
            new Metric(u.questionsAsked(), quota.maxQuestionsPerMonth()),
            new Metric(u.filesUploaded(), quota.maxFilesPerMonth()),
            new Metric(u.bytesStored(), quota.maxBytesStored()),
            new Metric(u.tokensConsumed(), quota.tokenWarnThreshold()));
    }

    public record Metric(long used, long limit) {}
    public record QuotaResponse(String period, Metric questions, Metric files,
                                Metric bytesStored, Metric tokens) {}

    private UserId principal(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserId userId))
            throw new Unauthenticated("authentication required");
        return userId;
    }
}
