package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.model.User;

public record RegistrationResult(User user, boolean created) {}
