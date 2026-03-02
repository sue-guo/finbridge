package io.github.sueguo.finbridge.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Hierarchical spending category.
 * e.g. FOOD (parent) -> DINING_OUT, GROCERIES (children)
 */
@Entity
@Table(name = "categories")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;          // e.g. "DINING_OUT"

    @Column(length = 60)
    private String displayName;   // e.g. "Dining Out"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;      // null = top-level

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Category> children = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseType expenseType;  // RECURRING or FLEXIBLE
}

