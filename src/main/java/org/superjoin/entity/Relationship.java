package org.superjoin.entity;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "relationships")
public class Relationship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    private String sourceEntityId;
    private String targetEntityId;
    private String relationshipType; // DEPENDS_ON, REFERENCES, CONTAINS, CALCULATES
    private String description;
    private Double strength; // Relationship strength (0.0 to 1.0)

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
}