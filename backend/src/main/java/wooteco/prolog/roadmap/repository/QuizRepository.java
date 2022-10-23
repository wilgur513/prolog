package wooteco.prolog.roadmap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wooteco.prolog.roadmap.domain.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

}
