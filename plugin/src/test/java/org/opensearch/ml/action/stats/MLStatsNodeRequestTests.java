/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.ml.action.stats;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.test.OpenSearchTestCase;

public class MLStatsNodeRequestTests extends OpenSearchTestCase {

    public void testSerializationDeserialization() throws IOException {
        MLStatsNodesRequest mlStatsNodesRequest = new MLStatsNodesRequest(("testNodeId"));
        mlStatsNodesRequest.clear();

        Set<String> statsToBeRetrieved = new HashSet<>(Arrays.asList("testStat"));

        for (String stat : statsToBeRetrieved) {
            mlStatsNodesRequest.addStat(stat);
        }
        mlStatsNodesRequest.addAll(statsToBeRetrieved);
        BytesStreamOutput output = new BytesStreamOutput();
        MLStatsNodeRequest request = new MLStatsNodeRequest(mlStatsNodesRequest);
        request.writeTo(output);
        MLStatsNodeRequest newRequest = new MLStatsNodeRequest(output.bytes().streamInput());
        Assert
            .assertEquals(
                newRequest.getMlStatsNodesRequest().getStatsToBeRetrieved().size(),
                request.getMlStatsNodesRequest().getStatsToBeRetrieved().size()
            );
        for (String stat : newRequest.getMlStatsNodesRequest().getStatsToBeRetrieved()) {
            Assert.assertTrue(request.getMlStatsNodesRequest().getStatsToBeRetrieved().contains(stat));
        }
    }
}
