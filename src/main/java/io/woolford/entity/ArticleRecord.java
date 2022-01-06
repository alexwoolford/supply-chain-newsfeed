package io.woolford.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@ToString
public class ArticleRecord {

    @Getter @Setter String link;
    @Getter @Setter private String title;
    @Getter @Setter private String description;
    @Getter @Setter private Date timestamp;

}
