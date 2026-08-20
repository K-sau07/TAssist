package com.tassist.application.auth;

import com.tassist.domain.error.EmailTaken;
import com.tassist.domain.error.InvalidCredentials;
import com.tassist.domain.error.NotFoundError;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.User;
import com.tassist.domain.port.out.UserRepository;
import com.tassist.domain.vo.AuthProvider;
import com.tassist.domain.vo.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Fast unit test: AuthService against an in-memory fake repo, real bcrypt encoder. */
class AuthServiceTest {

    private AuthService service;
    private FakeUserRepo repo;

    @BeforeEach
    void setUp() {
        repo = new FakeUserRepo();
        PasswordEncoder encoder = new BCryptPasswordEncoder(4); // low cost for test speed
        service = new AuthService(repo, encoder);
    }

    @Test
    void signup_creates_password_user_with_hashed_password() {
        User u = service.signupWithPassword("New@Example.com", "New User", "password123");
        assertThat(u.authProvider()).isEqualTo(AuthProvider.PASSWORD);
        assertThat(u.email()).isEqualTo("new@example.com"); // lowercased (D12)
        assertThat(u.passwordHash()).isPresent();
        assertThat(u.passwordHash().get()).isNotEqualTo("password123"); // hashed
    }

    @Test
    void signup_rejects_duplicate_email() {
        service.signupWithPassword("dup@example.com", "A", "password123");
        assertThatThrownBy(() -> service.signupWithPassword("DUP@example.com", "B", "password123"))
            .isInstanceOf(EmailTaken.class);
    }

    @Test
    void signup_validates_password_rules() {
        assertThatThrownBy(() -> service.signupWithPassword("a@b.com", "A", "short"))
            .isInstanceOf(ValidationError.class);
        assertThatThrownBy(() -> service.signupWithPassword("a@b.com", "A", "alllettersonly"))
            .isInstanceOf(ValidationError.class); // no digit
        assertThatThrownBy(() -> service.signupWithPassword("a@b.com", "A", "1234567890"))
            .isInstanceOf(ValidationError.class); // no letter
    }

    @Test
    void signup_validates_email_and_displayname() {
        assertThatThrownBy(() -> service.signupWithPassword("notanemail", "A", "password123"))
            .isInstanceOf(ValidationError.class);
        assertThatThrownBy(() -> service.signupWithPassword("a@b.com", "", "password123"))
            .isInstanceOf(ValidationError.class);
    }

    @Test
    void login_succeeds_with_correct_password() {
        service.signupWithPassword("login@example.com", "L", "password123");
        User u = service.loginWithPassword("Login@Example.com", "password123"); // case-insensitive email
        assertThat(u.email()).isEqualTo("login@example.com");
    }

    @Test
    void login_fails_with_wrong_password() {
        service.signupWithPassword("login2@example.com", "L", "password123");
        assertThatThrownBy(() -> service.loginWithPassword("login2@example.com", "wrongpass99"))
            .isInstanceOf(InvalidCredentials.class);
    }

    @Test
    void login_fails_for_unknown_email() {
        assertThatThrownBy(() -> service.loginWithPassword("ghost@example.com", "password123"))
            .isInstanceOf(InvalidCredentials.class);
    }

    @Test
    void upsertGoogleUser_creates_then_finds_same_user() {
        User first = service.upsertGoogleUser("google-sub-1", "g@example.com", "Google User");
        User second = service.upsertGoogleUser("google-sub-1", "g@example.com", "Google User");
        assertThat(first.id()).isEqualTo(second.id()); // idempotent by subject
        assertThat(first.authProvider()).isEqualTo(AuthProvider.GOOGLE);
    }

    @Test
    void getById_throws_when_missing() {
        assertThatThrownBy(() -> service.getById(UserId.newId()))
            .isInstanceOf(NotFoundError.class);
    }

    /** Minimal in-memory UserRepository for unit testing. */
    static class FakeUserRepo implements UserRepository {
        private final Map<UserId, User> byId = new HashMap<>();
        private final Map<String, User> byEmail = new HashMap<>();
        private final Map<String, User> bySub = new HashMap<>();

        @Override public User save(User u) {
            byId.put(u.id(), u);
            byEmail.put(u.email(), u);
            u.googleSubject().ifPresent(s -> bySub.put(s, u));
            return u;
        }
        @Override public Optional<User> findById(UserId id) { return Optional.ofNullable(byId.get(id)); }
        @Override public Optional<User> findByEmail(String email) { return Optional.ofNullable(byEmail.get(email)); }
        @Override public Optional<User> findByGoogleSubject(String sub) { return Optional.ofNullable(bySub.get(sub)); }
        @Override public boolean existsByEmail(String email) { return byEmail.containsKey(email); }
    }
}
