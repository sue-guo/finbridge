package io.github.sueguo.finbridge.model.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Keyword-based rule that maps a description substring to a Category.
 * Example: keyword="TIM HORTON" -> category=COFFEE
 * Rules are evaluated in ascending priority order (lower = higher priority).
 */
@Entity
@Table(name = "category_rules",
        indexes = @Index(name = "idx_rule_keyword", columnList = "keyword"))
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class CategoryRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String keyword;   // uppercase substring to match in description

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private int priority;     // lower number = higher priority
}
