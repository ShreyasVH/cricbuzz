package models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.ebean.Model;
import io.ebean.annotation.Cache;
import io.ebean.annotation.CacheQueryTuning;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "bowling_figures")
//@Cache(enableQueryCache=true)
//@CacheQueryTuning(maxSecsToLive = 3600)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BowlingFigure extends Model
{
    @Id
    private Long id;

    @ManyToOne
    @JsonBackReference
    private Match match;

    @ManyToOne
    private Player player;

    @ManyToOne
    private Team team;

    private int balls;

    private int maidens;

    private int runs;

    private int wickets;

    @Column(name = "innings_id")
    private int innings;

    @Column(name = "team_innings_id")
    private int teamInnings;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;
}
