package backend.academy.linktracker.scrapper.client.stackoverflow;

import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowQuestionItem;

public interface StackOverflowClient {

    StackOverflowQuestionItem getQuestion(long questionId);
}
