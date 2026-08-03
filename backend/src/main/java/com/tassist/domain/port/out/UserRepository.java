package com.tassist.domain.port.out;

import com.tassist.domain.model.User;
import com.tassist.domain.vo.UserId;
import java.util.Optional;

/** Persistence port for {@link User} (spec §7 out-port; adapter in Step 2). */
public interface UserRepository {
    User save(User user);
    Optional<User> findById(UserId id);
    Optional<User> findByEmail(String emailLowercased);
    Optional<User> findByGoogleSubject(String googleSubject);
    boolean existsByEmail(String emailLowercased);
}
