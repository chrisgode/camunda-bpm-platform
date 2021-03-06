/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.camunda.bpm.engine.impl.batch.deletion;

import org.camunda.bpm.engine.batch.Batch;
import org.camunda.bpm.engine.impl.batch.*;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.jobexecutor.JobDeclaration;
import org.camunda.bpm.engine.impl.json.JsonObjectConverter;
import org.camunda.bpm.engine.impl.persistence.entity.*;
import org.camunda.bpm.engine.impl.util.IoUtil;
import org.camunda.bpm.engine.impl.util.StringUtil;
import org.camunda.bpm.engine.impl.util.json.JSONObject;
import org.camunda.bpm.engine.impl.util.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

/**
 *
 * @author Askar Akhmerov
 */
public class DeleteProcessInstancesJobHandler extends AbstractBatchJobHandler<DeleteProcessInstanceBatchConfiguration> {
  public static final DeleteProcessInstancesBatchJobDeclaration JOB_DECLARATION = new DeleteProcessInstancesBatchJobDeclaration();

  @Override
  public String getType() {
    return Batch.TYPE_PROCESS_INSTANCE_DELETION;
  }

  protected DeleteProcessInstanceBatchConfigurationJsonConverter getJsonConverterInstance() {
    return DeleteProcessInstanceBatchConfigurationJsonConverter.INSTANCE;
  }

  @Override
  public JobDeclaration<?, MessageEntity> getJobDeclaration() {
    return JOB_DECLARATION;
  }

  @Override
  public boolean createJobs(BatchEntity batch) {
    CommandContext commandContext = Context.getCommandContext();
    ByteArrayManager byteArrayManager = commandContext.getByteArrayManager();
    JobManager jobManager = commandContext.getJobManager();

    DeleteProcessInstanceBatchConfiguration configuration = readConfiguration(batch.getConfigurationBytes());

    int batchJobsPerSeed = batch.getBatchJobsPerSeed();
    int invocationsPerBatchJob = batch.getInvocationsPerBatchJob();

    List<String> processInstanceIds = configuration.getProcessInstanceIds();
    int numberOfInstancesToProcess = Math.min(invocationsPerBatchJob * batchJobsPerSeed, processInstanceIds.size());
    // view of process instances to process
    List<String> processInstancesToProcess = processInstanceIds.subList(0, numberOfInstancesToProcess);

    int createdJobs = 0;
    while (!processInstancesToProcess.isEmpty()) {
      int lastIdIndex = Math.min(invocationsPerBatchJob, processInstancesToProcess.size());
      // view of process instances for this job
      List<String> idsForJob = processInstancesToProcess.subList(0, lastIdIndex);

      DeleteProcessInstanceBatchConfiguration jobConfiguration = DeleteProcessInstanceBatchConfiguration
          .create(idsForJob, configuration.getDeleteReason());
      ByteArrayEntity configurationEntity = saveConfiguration(byteArrayManager, jobConfiguration);

      JobEntity job = createBatchJob(batch, configurationEntity);
      jobManager.insertAndHintJobExecutor(job);

      idsForJob.clear();
      createdJobs++;
    }

    // update created jobs for batch
    batch.setJobsCreated(batch.getJobsCreated() + createdJobs);

    // update batch configuration
    batch.setConfigurationBytes(writeConfiguration(configuration));

    return processInstanceIds.isEmpty();
  }

  @Override
  public void execute(BatchJobConfiguration configuration, ExecutionEntity execution, CommandContext commandContext, String tenantId) {
    ByteArrayEntity configurationEntity = commandContext
        .getDbEntityManager()
        .selectById(ByteArrayEntity.class, configuration.getConfigurationByteArrayId());

    DeleteProcessInstanceBatchConfiguration batchConfiguration = readConfiguration(configurationEntity.getBytes());

    for (String pi : batchConfiguration.getProcessInstanceIds()) {
      commandContext.getProcessEngineConfiguration()
          .getRuntimeService()
          .deleteProcessInstance(pi,batchConfiguration.deleteReason,true,true);
    }


    commandContext.getByteArrayManager().delete(configurationEntity);
  }

  protected JobEntity createBatchJob(BatchEntity batch, ByteArrayEntity configuration) {
    BatchJobContext creationContext = new BatchJobContext(batch, configuration);
    return JOB_DECLARATION.createJobInstance(creationContext);
  }

}
