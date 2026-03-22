package backend.academy.linktracker.scrapper.repository.orm.jpa;

import backend.academy.linktracker.scrapper.repository.orm.entity.TgChatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TgChatJpaRepository extends JpaRepository<TgChatEntity, Long> {}
