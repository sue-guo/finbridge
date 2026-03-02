package io.github.sueguo.finbridge.repository;

import io.github.sueguo.finbridge.model.entity.CategoryRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRuleRepository extends JpaRepository<CategoryRule, Long> {
    /** Rules ordered by priority ascending — lowest number wins. */
    List<CategoryRule> findAllByOrderByPriorityAsc();
}