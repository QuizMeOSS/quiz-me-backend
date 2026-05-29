package com.quizme.repos;

import com.quizme.entities.User;
import com.quizme.entities.UserCredentials;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCredentialsRepo extends CrudRepository<UserCredentials, Long> {
    Optional<UserCredentials> findByUserId(User userId);
}
