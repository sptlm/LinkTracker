package backend.academy.linktracker.ai.service;

public interface Summarizer {

    String summarize(String text, int threshold);
}
