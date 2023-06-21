package com.udacity.webcrawler;

import com.google.common.collect.ImmutableList;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public final class CrawlAction extends RecursiveAction {
    private final Clock clock;
    private final PageParserFactory parserFactory;
    private final String url;
    private final Instant deadline;
    private final int maxDepth;
    private final ConcurrentHashMap<String, Integer> counts;
    private final ConcurrentSkipListSet<String> visitedUrls;
    private final List<Pattern> ignoredUrls;

    private CrawlAction(Clock clock,
                        PageParserFactory parserFactory,
                        String url,
                        Instant deadline,
                        int maxDepth,
                        ConcurrentHashMap<String, Integer> counts,
                        ConcurrentSkipListSet<String> visitedUrls,
                        List<Pattern> ignoredUrls) {
        this.clock = clock;
        this.parserFactory = parserFactory;
        this.url = url;
        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.ignoredUrls = ignoredUrls;
    }

    public static final class Builder {
        private Clock clock;
        private PageParserFactory parserFactory;
        private String url;
        private Instant deadline;
        private int maxDepth;
        private ConcurrentHashMap<String, Integer> counts;
        private ConcurrentSkipListSet<String> visitedUrls;
        private List<Pattern> ignoredUrls;

        public Builder setClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder setParserFactory(PageParserFactory parserFactory) {
            this.parserFactory = parserFactory;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setDeadline(Instant deadline) {
            this.deadline = deadline;
            return this;
        }

        public Builder setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder setCounts(ConcurrentHashMap<String, Integer> counts) {
            this.counts = counts;
            return this;
        }

        public Builder setVisitedUrls(ConcurrentSkipListSet<String> visitedUrls) {
            this.visitedUrls = visitedUrls;
            return this;
        }

        public Builder setIgnoredUrls(List<Pattern> ignoredUrls) {
            this.ignoredUrls = ignoredUrls;
            return this;
        }

        public CrawlAction build() {
            return new CrawlAction(clock, parserFactory, url, deadline, maxDepth, counts, visitedUrls, ignoredUrls);
        }
    }

    @Override
    protected void compute() {
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }
        if(!visitedUrls.add(url)) {
            return;
        }
        PageParser.Result result = parserFactory.get(url).parse();
        for (ConcurrentHashMap.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            counts.compute(e.getKey(), (k, v) -> (v == null) ? e.getValue() : v + e.getValue());
        }


        List<CrawlAction> subactions = result
                .getLinks()
                .stream()
                .map(link -> new CrawlAction.Builder()
                        .setClock(clock)
                        .setParserFactory(parserFactory)
                        .setUrl(link)
                        .setDeadline(deadline)
                        .setMaxDepth(maxDepth - 1)
                        .setCounts(counts)
                        .setVisitedUrls(visitedUrls)
                        .setIgnoredUrls(ignoredUrls)
                        .build())
                .collect(Collectors.toList());
        invokeAll(subactions);
    }
}
