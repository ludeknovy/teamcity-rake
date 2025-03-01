/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.agent.rakerunner.scripting;

import com.intellij.openapi.diagnostic.Logger;
import java.io.File;
import java.io.PrintStream;
import java.util.Map;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.agent.rakerunner.utils.EnvironmentPatchableMap;
import jetbrains.buildServer.agent.rakerunner.utils.RunnerUtil;
import jetbrains.buildServer.agent.ruby.RubySdk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.rvm.RVMSupportUtil;

/**
 * @author Roman.Chernyatchik
 * @author Vladislav.Rassokhin
 */
public class ProcessBasedRubyScriptRunner implements RubyScriptRunner {
  private static final Logger LOG = Logger.getInstance(ProcessBasedRubyScriptRunner.class.getName());

  public ProcessBasedRubyScriptRunner(@NotNull final RubySdk sdk) {
    mySdk = sdk;
  }

  private final RubySdk mySdk;

  @NotNull
  public ExecResult run(@NotNull final String script,
                        @NotNull final String workingDirectory,
                        @Nullable final Map<String, String> environment,
                        @NotNull final String... rubyArgs) {
    ExecResult result = null;
    File scriptFile = null;
    try {
      // Writing source to the temp file
      scriptFile = File.createTempFile("script", ".rb");
      PrintStream out = new PrintStream(scriptFile);
      out.print(script);
      out.close();

      //Args
      final String[] args = new String[2 + rubyArgs.length];
      args[0] = mySdk.getRubyExecutable().getAbsolutePath();
      System.arraycopy(rubyArgs, 0, args, 1, rubyArgs.length);
      args[rubyArgs.length + 1] = scriptFile.getPath();

      // Env
      final EnvironmentPatchableMap patchableEnv = new EnvironmentPatchableMap(environment);
      RVMSupportUtil.patchEnvForRVMIfNecessary(mySdk, patchableEnv);

      //Result
      result = RunnerUtil.run(workingDirectory, patchableEnv, args);
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    } finally {
      if (scriptFile != null && scriptFile.exists()) {
        scriptFile.delete();
      }
    }

    return result;
  }
}
