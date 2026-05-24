package backend.academy.linktracker.ai.service;

import org.springframework.stereotype.Component;

@Component
public class TruncatingSummarizer implements Summarizer {

    private static final String ELLIPSIS = "...";

    @Override
    public String summarize(String text, int threshold) {
        if (text == null || text.length() <= threshold) {
            return text;
        }
        if (threshold <= ELLIPSIS.length()) {
            return text.substring(0, threshold);
        }
        return text.substring(0, threshold - ELLIPSIS.length()) + ELLIPSIS;
    }
}
