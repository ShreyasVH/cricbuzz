package services.impl;

import com.google.inject.Inject;
import enums.ErrorCode;
import exceptions.BadRequestException;
import exceptions.DBInteractionException;
import exceptions.NotFoundException;
import io.ebean.Ebean;
import io.ebean.Transaction;
import models.*;
import org.springframework.util.StringUtils;
import repositories.*;
import requests.series.CreateRequest;
import requests.series.UpdateRequest;
import services.SeriesService;
import utils.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class SeriesServiceImpl implements SeriesService
{
    private final CountryRepository countryRepository;
    private final PlayerRepository playerRepository;
    private final SeriesRepository seriesRepository;
    private final TeamRepository teamRepository;
    private final TourRepository tourRepository;

    @Inject
    public SeriesServiceImpl
    (
        CountryRepository countryRepository,
        PlayerRepository playerRepository,
        SeriesRepository seriesRepository,
        TeamRepository teamRepository,
        TourRepository tourRepository
    )
    {
        this.countryRepository = countryRepository;
        this.playerRepository = playerRepository;
        this.seriesRepository = seriesRepository;
        this.teamRepository = teamRepository;
        this.tourRepository = tourRepository;
    }

    @Override
    public CompletionStage<List<Series>> getAll()
    {
        return this.seriesRepository.getAll();
    }

    @Override
    public Series get(Long id)
    {
        Series series = this.seriesRepository.get(id);
        if(null == series)
        {
            throw new NotFoundException(ErrorCode.NOT_FOUND.getCode(), String.format(ErrorCode.NOT_FOUND.getDescription(), "Series"));
        }

        return series;
    }

    @Override
    public List<Series> get(String keyword) {
        return this.seriesRepository.get(keyword);
    }

    @Override
    public Series create(CreateRequest createRequest)
    {
        createRequest.validate();
        Series existingSeries = this.seriesRepository.get(createRequest.getName(), createRequest.getGameType());
        if(null != existingSeries)
        {
            throw new BadRequestException(ErrorCode.ALREADY_EXISTS.getCode(), ErrorCode.ALREADY_EXISTS.getDescription());
        }

        Country homeCountry = this.countryRepository.get(createRequest.getHomeCountryId());
        if(null == homeCountry)
        {
            throw new BadRequestException(ErrorCode.NOT_FOUND.getCode(), String.format(ErrorCode.NOT_FOUND.getDescription(), "Country"));
        }

        Transaction transaction = Ebean.beginTransaction();
        try
        {
            Series series = new Series();
            series.setHomeCountry(homeCountry);
            series.setName(createRequest.getName());
            series.setType(createRequest.getType());
            series.setGameType(createRequest.getGameType());
            try
            {

                series.setStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(createRequest.getStartTime()));
                series.setEndTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(createRequest.getEndTime()));

            }
            catch(Exception ex)
            {
                throw new BadRequestException(ErrorCode.INVALID_REQUEST.getCode(), ErrorCode.INVALID_REQUEST.getDescription());
            }

            Tour tour = this.tourRepository.get(createRequest.getTourId());
            if(null == tour)
            {
                throw new NotFoundException(ErrorCode.NOT_FOUND.getCode(), String.format(ErrorCode.NOT_FOUND.getDescription(), "Tour"));
            }
            series.setTour(tour);

            Date now = Utils.getCurrentDate();
            series.setCreatedAt(now);
            series.setUpdatedAt(now);

            List<Team> teams = this.teamRepository.get(createRequest.getTeams());
            if(createRequest.getTeams().size() != teams.size())
            {
                throw new BadRequestException(ErrorCode.INVALID_REQUEST.getCode(), ErrorCode.INVALID_REQUEST.getDescription());
            }

            series.setTeams(teams);
            Series createdSeries = this.seriesRepository.save(series);
            transaction.commit();
            transaction.end();
            return createdSeries;
        }
        catch(Exception ex)
        {
            transaction.rollback();
            transaction.end();
            throw new DBInteractionException(ErrorCode.DB_INTERACTION_FAILED.getCode(), ErrorCode.DB_INTERACTION_FAILED.getDescription());
        }
    }

    @Override
    public Series update(Long id, UpdateRequest updateRequest)
    {
        Series existingSeries = this.seriesRepository.get(id);
        if(null == existingSeries)
        {
            throw new NotFoundException(ErrorCode.NOT_FOUND.getCode(), String.format(ErrorCode.NOT_FOUND.getDescription(), "Series"));
        }
        updateRequest.validate(existingSeries);

        Transaction transaction = Ebean.beginTransaction();
        try
        {
            boolean isUpdateRequired = false;
            existingSeries.setUpdatedAt(Utils.getCurrentDate());

            if(!StringUtils.isEmpty(updateRequest.getName()) && !existingSeries.getName().equals(updateRequest.getName()))
            {
                isUpdateRequired = true;
                existingSeries.setName(updateRequest.getName());
            }

            if((null != updateRequest.getType()) && !existingSeries.getType().equals(updateRequest.getType()))
            {
                isUpdateRequired = true;
                existingSeries.setType(updateRequest.getType());
            }

            if((null != updateRequest.getGameType()) && !existingSeries.getGameType().equals(updateRequest.getGameType()))
            {
                isUpdateRequired = true;
                existingSeries.setGameType(updateRequest.getGameType());
            }

            if(!StringUtils.isEmpty(updateRequest.getStartTime()))
            {
                try
                {
                    Date startTime = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(updateRequest.getStartTime()));
                    if(startTime.getTime() != existingSeries.getStartTime().getTime())
                    {
                        isUpdateRequired = true;
                        existingSeries.setStartTime(startTime);
                    }
                }
                catch(Exception ex)
                {
                    throw new BadRequestException(ErrorCode.INVALID_REQUEST.getCode(), ErrorCode.INVALID_REQUEST.getDescription());
                }
            }

            if(!StringUtils.isEmpty(updateRequest.getEndTime()))
            {
                try
                {
                    Date endTime = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(updateRequest.getEndTime()));
                    if(endTime.getTime() != existingSeries.getEndTime().getTime())
                    {
                        isUpdateRequired = true;
                        existingSeries.setStartTime(endTime);
                    }
                }
                catch(Exception ex)
                {
                    throw new BadRequestException(ErrorCode.INVALID_REQUEST.getCode(), ErrorCode.INVALID_REQUEST.getDescription());
                }
            }

            if((null != updateRequest.getHomeCountryId()) && (!updateRequest.getHomeCountryId().equals(existingSeries.getHomeCountry().getId())))
            {
                Country country = this.countryRepository.get(updateRequest.getHomeCountryId());
                if(null == country)
                {
                    throw new BadRequestException(ErrorCode.NOT_FOUND.getCode(), String.format(ErrorCode.NOT_FOUND.getDescription(), "Country"));
                }

                isUpdateRequired = true;
                existingSeries.setHomeCountry(country);
            }

            if((null != updateRequest.getTourId()) && !updateRequest.getTourId().equals(existingSeries.getTour().getId()))
            {
                Tour tour = this.tourRepository.get(updateRequest.getTourId());
                if(null == tour)
                {
                    throw new NotFoundException(ErrorCode.NOT_FOUND.getCode(), String.format(ErrorCode.NOT_FOUND.getDescription(), "Tour"));
                }
                isUpdateRequired = true;
                existingSeries.setTour(tour);
            }

            if((null != updateRequest.getTeams()) && (updateRequest.getTeams().size() > 0))
            {
                List<Team> teams = this.teamRepository.get(updateRequest.getTeams());
                if(teams.size() != updateRequest.getTeams().size())
                {
                    throw new BadRequestException(ErrorCode.INVALID_REQUEST.getCode(), ErrorCode.INVALID_REQUEST.getDescription());
                }

                isUpdateRequired = true;
                existingSeries.getTeams().clear();
                existingSeries.getTeams().addAll(teams);
            }

            if((null != updateRequest.getManOfTheSeriesList()) && (!updateRequest.getManOfTheSeriesList().isEmpty()))
            {
                List<ManOfTheSeries> manOfTheSeriesList = new ArrayList<>();
                for(Map<String, Long> manOfTheSeriesRaw: updateRequest.getManOfTheSeriesList())
                {
                    Long playerId = manOfTheSeriesRaw.get("playerId");
                    Long teamId = manOfTheSeriesRaw.get("teamId");
                    ManOfTheSeries manOfTheSeries = new ManOfTheSeries();

                    Player player = this.playerRepository.get(playerId);
                    if(null == player)
                    {
                        throw new NotFoundException(ErrorCode.NOT_FOUND.getCode(), String.format(ErrorCode.NOT_FOUND.getDescription(), "Player"));
                    }

                    manOfTheSeries.setPlayer(player);

                    Team team = this.teamRepository.get(teamId);
                    if(null == team)
                    {
                        throw new NotFoundException(ErrorCode.NOT_FOUND.getCode(), String.format(ErrorCode.NOT_FOUND.getDescription(), "Team"));
                    }
                    manOfTheSeries.setTeam(team);
                    manOfTheSeries.setSeries(existingSeries);

                    manOfTheSeriesList.add(manOfTheSeries);
                }
                isUpdateRequired = true;
                existingSeries.getManOfTheSeriesList().clear();
                existingSeries.getManOfTheSeriesList().addAll(manOfTheSeriesList);
            }

            Series updatedSeries = null;
            if(isUpdateRequired)
            {
                updatedSeries = this.seriesRepository.save(existingSeries);
                transaction.commit();
                transaction.end();
            }
            return updatedSeries;
        }
        catch(Exception ex)
        {
            transaction.rollback();
            transaction.end();
            throw new DBInteractionException(ErrorCode.DB_INTERACTION_FAILED.getCode(), ErrorCode.DB_INTERACTION_FAILED.getDescription());
        }
    }
}
