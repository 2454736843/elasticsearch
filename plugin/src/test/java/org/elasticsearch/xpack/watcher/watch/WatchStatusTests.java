/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.watch;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.watcher.actions.ActionStatus;
import org.elasticsearch.xpack.watcher.actions.ActionStatus.AckStatus.State;
import org.elasticsearch.xpack.watcher.actions.logging.LoggingAction;
import org.elasticsearch.xpack.watcher.support.xcontent.WatcherParams;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.joda.time.DateTime.now;

public class WatchStatusTests extends ESTestCase {

    public void testAckStatusIsResetOnUnmetCondition() {
        HashMap<String, ActionStatus> myMap = new HashMap<>();
        ActionStatus actionStatus = new ActionStatus(now());
        myMap.put("foo", actionStatus);

        actionStatus.update(now(), new LoggingAction.Result.Success("foo"));
        actionStatus.onAck(now());
        assertThat(actionStatus.ackStatus().state(), is(State.ACKED));

        WatchStatus status = new WatchStatus(now(), myMap);
        status.onCheck(false, now());

        assertThat(status.actionStatus("foo").ackStatus().state(), is(State.AWAITS_SUCCESSFUL_EXECUTION));
    }

    public void testHeadersToXContent() throws Exception {
        WatchStatus status = new WatchStatus(now(), Collections.emptyMap());
        String key = randomAlphaOfLength(10);
        String value = randomAlphaOfLength(10);
        Map<String, String> headers = Collections.singletonMap(key, value);
        status.setHeaders(headers);

        // by default headers are hidden
        try (XContentBuilder builder = jsonBuilder()) {
            status.toXContent(builder, ToXContent.EMPTY_PARAMS);
            try (XContentParser parser = createParser(builder)) {
                Map<String, Object> fields = parser.map();
                assertThat(fields, not(hasKey(WatchStatus.Field.HEADERS.getPreferredName())));
            }
        }

        // but they are required when storing a watch
        try (XContentBuilder builder = jsonBuilder()) {
            status.toXContent(builder, WatcherParams.builder().hideHeaders(false).build());
            try (XContentParser parser = createParser(builder)) {
                parser.nextToken();
                Map<String, Object> fields = parser.map();
                assertThat(fields, hasKey(WatchStatus.Field.HEADERS.getPreferredName()));
                assertThat(fields.get(WatchStatus.Field.HEADERS.getPreferredName()), instanceOf(Map.class));
                Map<String, Object> extractedHeaders = (Map<String, Object>) fields.get(WatchStatus.Field.HEADERS.getPreferredName());
                assertThat(extractedHeaders, is(headers));
            }
        }
    }
}