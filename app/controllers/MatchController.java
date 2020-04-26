package controllers;

import com.google.inject.Inject;
import enums.ErrorCode;
import exceptions.BadRequestException;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import requests.matches.CreateRequest;
import services.MatchService;
import utils.Utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class MatchController extends Controller
{
    private final MatchService matchService;

    private final HttpExecutionContext httpExecutionContext;

    @Inject
    public MatchController
    (
        MatchService matchService,

        HttpExecutionContext httpExecutionContext
    )
    {
        this.matchService = matchService;

        this.httpExecutionContext = httpExecutionContext;
    }

    public CompletionStage<Result> get(Long id)
    {
        return CompletableFuture.supplyAsync(() -> this.matchService.get(id)).thenApplyAsync(match -> ok(Json.toJson(match)), this.httpExecutionContext.current());
    }

    public CompletableFuture<Result> create(Http.Request request)
    {
        return CompletableFuture.supplyAsync(() -> {
            CreateRequest createRequest;
            try
            {
                createRequest = Utils.convertObject(request.body().asJson(), CreateRequest.class);
            }
            catch(Exception ex)
            {
                throw new BadRequestException(ErrorCode.INVALID_REQUEST.getCode(), ErrorCode.INVALID_REQUEST.getDescription());
            }

            return this.matchService.create(createRequest);
        }, this.httpExecutionContext.current()).thenApplyAsync(match -> ok(Json.toJson(match)), this.httpExecutionContext.current());
    }
}
