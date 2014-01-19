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
package org.camunda.bpm.qa.performance.engine;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.camunda.bpm.qa.performance.engine.framework.PerfTestException;
import org.camunda.bpm.qa.performance.engine.framework.PerfTestResults;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * JUnit rule recording the performance test result
 *
 * @author Daniel Meyer
 *
 */
public class PerformanceTestResultRecorderRule extends TestWatcher {

  public static final Logger LOG = Logger.getLogger(PerformanceTestResultRecorderRule.class.getName());

  protected PerfTestResults results;

  @Override
  protected void succeeded(Description description) {
    if(results != null) {
      results.setTestName(description.getClassName() +"."+description.getMethodName());
      LOG.log(Level.INFO, results.toString());

      String resultFileName = formatResultFileName(description);

      try {
        // create file:
        File directory = new File(formatResultFileDirName());
        if (!directory.exists()) {
          directory.mkdir();
        }
        File testResults = new File(resultFileName);
        if(testResults.exists()) {
          testResults.delete();
        }
        testResults.createNewFile();

        // write to file:
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(testResults, results);
      } catch ( Exception e ){
        throw new PerfTestException("Could not record results to file "+resultFileName, e);

      }
    }
  }

  protected String formatResultFileDirName() {
    return "target"+File.separatorChar + "results";
  }

  protected String formatResultFileName(Description description) {
    return formatResultFileDirName() + File.separatorChar + description.getTestClass().getSimpleName() + "."+description.getMethodName() +".json";
  }

  public void setResults(PerfTestResults results) {
    this.results = results;
  }

}
