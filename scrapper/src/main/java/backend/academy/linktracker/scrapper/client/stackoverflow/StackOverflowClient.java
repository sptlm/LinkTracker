package backend.academy.linktracker.scrapper.client.stackoverflow;

import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowAnswerItem;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowCommentItem;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowQuestionItem;
import java.util.List;

public interface StackOverflowClient {

    StackOverflowQuestionItem getQuestion(long questionId);

    List<StackOverflowAnswerItem> getLatestAnswers(long questionId, int limit);

    List<StackOverflowCommentItem> getLatestComments(long questionId, int limit);
}
