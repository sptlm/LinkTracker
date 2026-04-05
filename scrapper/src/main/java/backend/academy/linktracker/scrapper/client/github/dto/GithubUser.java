package backend.academy.linktracker.scrapper.client.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubUser(@JsonProperty("login") String login) {}
