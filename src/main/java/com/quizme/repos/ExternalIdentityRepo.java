package com.quizme.repos;

import com.quizme.entities.ExternalIdentity;
import com.quizme.entities.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExternalIdentityRepo extends CrudRepository<ExternalIdentity, Long> {
    List<ExternalIdentity> findByUserId(User userId);
}
