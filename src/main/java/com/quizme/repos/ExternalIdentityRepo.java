package com.quizme.repos;

import com.quizme.entities.ExternalIdentity;
import com.quizme.entities.User;
import com.quizme.entities.UserCredentials;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExternalIdentityRepo extends CrudRepository<ExternalIdentity, Long> {
    Optional<ExternalIdentity> findByUserId(User userId);
}
