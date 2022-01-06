package io.woolford.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@ToString
public class EntityRecord {

    @Getter @Setter private String link;
    @Getter @Setter private String entityType;
    @Getter @Setter private String entityValue;
    @Getter @Setter private Date timestamp;

}
