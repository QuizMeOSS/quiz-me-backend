package com.quizme.repos;

import com.quizme.entities.User;
import com.quizme.entities.UserCredentials;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCredentialsRepo extends CrudRepository<UserCredentials, Long> {
    Optional<UserCredentials> findByUserId(User userId);

    @Modifying
    @Query(value = """
            UPDATE user_credentials
            SET email_verified = true
            WHERE id = :id
            """,
            nativeQuery = true)
    void verifyEmail(@Param("id") Long credentialsId);
}
