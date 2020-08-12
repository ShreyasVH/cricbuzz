package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import enums.GameType;
import enums.SeriesType;
import io.ebean.Model;
import io.ebean.annotation.Cache;
import io.ebean.annotation.CacheQueryTuning;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Setter
@Getter
@Table(name = "series")
@Cache(enableQueryCache=true)
@CacheQueryTuning(maxSecsToLive = 3600)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Series extends Model
{
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @ManyToOne
    private Country homeCountry;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "series_teams_map",
        joinColumns = @JoinColumn(name = "series_id", referencedColumnName = "id"),
        inverseJoinColumns = @JoinColumn(name = "team_id", referencedColumnName = "id")
    )
    private List<Team> teams = new ArrayList<>();

    @OrderBy("startTime asc")
    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("series")
    private List<Match> matches;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "tour_id", referencedColumnName = "id")
    @JsonIgnoreProperties("seriesList")
    private Tour tour;

    @Column(nullable = false)
    private SeriesType type;

    @Column(name = "game_type", nullable = false)
    private GameType gameType;

    @Column(name = "start_time", nullable = false)
    private Date startTime;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(referencedColumnName = "id", name = "series_id")
    private List<ManOfTheSeries> manOfTheSeriesList;
}
