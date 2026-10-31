
package com.ai.SpringAiDemo.chat;

import com.ai.SpringAiDemo.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatOwnerRepository
        extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);
}
