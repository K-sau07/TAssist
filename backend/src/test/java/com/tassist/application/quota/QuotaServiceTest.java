package com.tassist.application.quota;

import com.tassist.domain.error.QuotaError;
import com.tassist.domain.model.QuotaUsage;
import com.tassist.domain.port.out.QuotaUsageRepository;
import com.tassist.domain.vo.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/** Unit tests for §16.2 quota enforcement, fake repo. */
class QuotaServiceTest {

    static class FakeQuotas implements QuotaUsageRepository {
        QuotaUsage stored;
        public QuotaUsage save(QuotaUsage q) { stored = q; return q; }
        public Optional<QuotaUsage> find(UserId u, YearMonth p) { return Optional.ofNullable(stored); }
    }

    FakeQuotas repo;
    QuotaService svc;
    UserId user;

    @BeforeEach void setup() {
        repo = new FakeQuotas();
        // small limits for testing: 2 questions, 2 files, 1000 bytes, warn 100
        svc = new QuotaService(repo, 2, 1000, 2, 100);
        user = UserId.newId();
    }

    @Test void allows_questions_under_limit() {
        assertThatCode(() -> svc.checkQuestionAllowed(user)).doesNotThrowAnyException();
    }

    @Test void blocks_questions_at_limit() {
        repo.save(new QuotaUsage(user, YearMonth.now(), 2, 0, 0, 0)); // already at max=2
        assertThatThrownBy(() -> svc.checkQuestionAllowed(user))
            .isInstanceOf(QuotaError.QuotaExceeded.class);
    }

    @Test void record_question_increments_and_adds_tokens() {
        svc.recordQuestion(user, 50);
        assertThat(repo.stored.questionsAsked()).isEqualTo(1);
        assertThat(repo.stored.tokensConsumed()).isEqualTo(50);
    }

    @Test void blocks_upload_at_file_limit() {
        repo.save(new QuotaUsage(user, YearMonth.now(), 0, 2, 0, 0)); // at file max=2
        assertThatThrownBy(() -> svc.checkUploadAllowed(user, 10))
            .isInstanceOf(QuotaError.QuotaExceeded.class);
    }

    @Test void blocks_upload_exceeding_storage() {
        repo.save(new QuotaUsage(user, YearMonth.now(), 0, 0, 950, 0)); // 950 + 100 > 1000
        assertThatThrownBy(() -> svc.checkUploadAllowed(user, 100))
            .isInstanceOf(QuotaError.QuotaExceeded.class);
    }

    @Test void allows_upload_within_limits() {
        repo.save(new QuotaUsage(user, YearMonth.now(), 0, 1, 500, 0));
        assertThatCode(() -> svc.checkUploadAllowed(user, 100)).doesNotThrowAnyException();
    }

    @Test void record_upload_increments_files_and_bytes() {
        svc.recordUpload(user, 200);
        assertThat(repo.stored.filesUploaded()).isEqualTo(1);
        assertThat(repo.stored.bytesStored()).isEqualTo(200);
    }
}
