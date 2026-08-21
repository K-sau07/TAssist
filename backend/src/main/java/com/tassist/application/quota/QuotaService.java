package com.tassist.application.quota;

import com.tassist.domain.error.QuotaError;
import com.tassist.domain.model.QuotaUsage;
import com.tassist.domain.port.in.QuotaUseCase;
import com.tassist.domain.port.out.QuotaUsageRepository;
import com.tassist.domain.vo.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

/**
 * Monthly quota tracking + enforcement (§16.2). Single default tier in Phase 1.
 * check* methods throw QuotaError.QuotaExceeded; record* methods upsert the (userId, period) row.
 * tokensConsumed is warn-only in Phase 1 (not hard-blocked).
 */
@Service
public class QuotaService implements QuotaUseCase {

    private static final Logger log = LoggerFactory.getLogger(QuotaService.class);

    private final QuotaUsageRepository quotas;
    private final long maxFilesPerMonth;
    private final long maxBytesStored;
    private final long maxQuestionsPerMonth;
    private final long tokenWarnThreshold;

    public QuotaService(QuotaUsageRepository quotas,
                        @Value("${tassist.quota.max-files-per-month:50}") long maxFilesPerMonth,
                        @Value("${tassist.quota.max-bytes-stored:524288000}") long maxBytesStored, // 500 MB
                        @Value("${tassist.quota.max-questions-per-month:500}") long maxQuestionsPerMonth,
                        @Value("${tassist.quota.token-warn-threshold:1000000}") long tokenWarnThreshold) {
        this.quotas = quotas;
        this.maxFilesPerMonth = maxFilesPerMonth;
        this.maxBytesStored = maxBytesStored;
        this.maxQuestionsPerMonth = maxQuestionsPerMonth;
        this.tokenWarnThreshold = tokenWarnThreshold;
    }

    @Override
    public QuotaUsage getCurrent(UserId user) {
        return current(user);
    }

    @Override
    public void checkQuestionAllowed(UserId user) {
        QuotaUsage u = current(user);
        if (u.questionsAsked() >= maxQuestionsPerMonth)
            throw new QuotaError.QuotaExceeded(
                "Monthly question limit reached (" + u.questionsAsked() + "/" + maxQuestionsPerMonth + ").");
    }

    @Override
    public void checkUploadAllowed(UserId user, long sizeBytes) {
        QuotaUsage u = current(user);
        if (u.filesUploaded() >= maxFilesPerMonth)
            throw new QuotaError.QuotaExceeded(
                "Monthly upload limit reached (" + u.filesUploaded() + "/" + maxFilesPerMonth + ").");
        if (u.bytesStored() + Math.max(0, sizeBytes) > maxBytesStored)
            throw new QuotaError.QuotaExceeded(
                "Storage limit reached (" + u.bytesStored() + "+" + sizeBytes + " > " + maxBytesStored + " bytes).");
    }

    @Override
    @Transactional
    public void recordQuestion(UserId user, long tokensConsumed) {
        QuotaUsage u = current(user);
        long tokens = u.tokensConsumed() + Math.max(0, tokensConsumed);
        if (tokens >= tokenWarnThreshold && u.tokensConsumed() < tokenWarnThreshold)
            log.warn("User {} crossed token warn threshold: {} tokens this month", user.value(), tokens);
        quotas.save(new QuotaUsage(user, u.period(), u.questionsAsked() + 1,
            u.filesUploaded(), u.bytesStored(), tokens));
    }

    @Override
    @Transactional
    public void recordUpload(UserId user, long sizeBytes) {
        QuotaUsage u = current(user);
        quotas.save(new QuotaUsage(user, u.period(), u.questionsAsked(),
            u.filesUploaded() + 1, u.bytesStored() + Math.max(0, sizeBytes), u.tokensConsumed()));
    }

    // Limits exposed for GET /api/quota.
    public long maxFilesPerMonth() { return maxFilesPerMonth; }
    public long maxBytesStored() { return maxBytesStored; }
    public long maxQuestionsPerMonth() { return maxQuestionsPerMonth; }
    public long tokenWarnThreshold() { return tokenWarnThreshold; }

    private QuotaUsage current(UserId user) {
        YearMonth period = YearMonth.now();
        return quotas.find(user, period).orElse(new QuotaUsage(user, period, 0, 0, 0, 0));
    }
}
