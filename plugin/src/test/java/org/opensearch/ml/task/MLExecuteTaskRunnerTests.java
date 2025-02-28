/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.ml.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opensearch.action.ActionListener;
import org.opensearch.client.Client;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.ml.common.FunctionName;
import org.opensearch.ml.common.breaker.MLCircuitBreakerService;
import org.opensearch.ml.common.input.execute.samplecalculator.LocalSampleCalculatorInput;
import org.opensearch.ml.common.transport.execute.MLExecuteTaskRequest;
import org.opensearch.ml.common.transport.execute.MLExecuteTaskResponse;
import org.opensearch.ml.indices.MLInputDatasetHandler;
import org.opensearch.ml.stats.MLStat;
import org.opensearch.ml.stats.MLStats;
import org.opensearch.ml.stats.StatNames;
import org.opensearch.ml.stats.suppliers.CounterSupplier;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

public class MLExecuteTaskRunnerTests extends OpenSearchTestCase {

    @Mock
    ThreadPool threadPool;

    @Mock
    ClusterService clusterService;

    @Mock
    Client client;

    @Mock
    MLTaskManager mlTaskManager;

    @Mock
    ExecutorService executorService;

    @Mock
    MLTaskDispatcher mlTaskDispatcher;

    @Mock
    MLCircuitBreakerService mlCircuitBreakerService;

    @Mock
    TransportService transportService;

    @Mock
    ActionListener<MLExecuteTaskResponse> listener;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    MLInputDatasetHandler mlInputDatasetHandler;
    MLExecuteTaskRunner taskRunner;
    MLStats mlStats;
    MLExecuteTaskRequest mlExecuteTaskRequest;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        when(threadPool.executor(anyString())).thenReturn(executorService);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));

        Map<String, MLStat<?>> stats = new ConcurrentHashMap<>();
        stats.put(StatNames.ML_EXECUTING_TASK_COUNT, new MLStat<>(false, new CounterSupplier()));
        stats.put(StatNames.ML_TOTAL_REQUEST_COUNT, new MLStat<>(false, new CounterSupplier()));
        stats.put(StatNames.ML_TOTAL_FAILURE_COUNT, new MLStat<>(false, new CounterSupplier()));
        stats.put(StatNames.ML_TOTAL_MODEL_COUNT, new MLStat<>(false, new CounterSupplier()));
        this.mlStats = new MLStats(stats);

        mlInputDatasetHandler = spy(new MLInputDatasetHandler(client));
        taskRunner = spy(
            new MLExecuteTaskRunner(
                threadPool,
                clusterService,
                client,
                mlTaskManager,
                mlStats,
                mlInputDatasetHandler,
                mlTaskDispatcher,
                mlCircuitBreakerService
            )
        );

        mlExecuteTaskRequest = new MLExecuteTaskRequest(
            FunctionName.LOCAL_SAMPLE_CALCULATOR,
            new LocalSampleCalculatorInput("sum", Arrays.asList(1.0, 2.0))
        );
    }

    public void testExecuteTask_Success() {
        taskRunner.executeTask(mlExecuteTaskRequest, listener);
        verify(listener).onResponse(any(MLExecuteTaskResponse.class));
    }

    public void testExecuteTask_NoExecutorService() {
        exceptionRule.expect(IllegalArgumentException.class);
        when(threadPool.executor(anyString())).thenThrow(new IllegalArgumentException());
        taskRunner.executeTask(mlExecuteTaskRequest, listener);
        verify(listener, never()).onResponse(any(MLExecuteTaskResponse.class));
    }
}
