package com.quizme.repos;

import com.quizme.entities.QuestionChoice;
import com.quizme.entities.QuestionChoiceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionChoiceRepo extends JpaRepository<QuestionChoice, QuestionChoiceId> {

}