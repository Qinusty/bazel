// Copyright 2014 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.rules.java;

import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.analysis.RuleConfiguredTarget.Mode;
import com.google.devtools.build.lib.analysis.RuleContext;
import com.google.devtools.build.lib.analysis.TransitiveInfoCollection;
import com.google.devtools.build.lib.packages.BuildType;
import com.google.devtools.build.lib.shell.ShellUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for use by Java-related parts of Bazel.
 */
// TODO(bazel-team): Merge with JavaUtil.
public abstract class JavaHelper {

  private JavaHelper() {}

  /**
   * Returns the java launcher implementation for the given target, if any.
   * A null return value means "use the JDK launcher".
   */
  public static TransitiveInfoCollection launcherForTarget(JavaSemantics semantics,
      RuleContext ruleContext) {
    String launcher = filterLauncherForTarget(semantics, ruleContext);
    return (launcher == null) ? null : ruleContext.getPrerequisite(launcher, Mode.TARGET);
  }

  /**
   * Returns the java launcher artifact for the given target, if any.
   * A null return value means "use the JDK launcher".
   */
  public static Artifact launcherArtifactForTarget(JavaSemantics semantics,
      RuleContext ruleContext) {
    String launcher = filterLauncherForTarget(semantics, ruleContext);
    return (launcher == null) ? null : ruleContext.getPrerequisiteArtifact(launcher, Mode.TARGET);
  }

  /**
   * Control structure abstraction for safely extracting a prereq from the launcher attribute
   * or --java_launcher flag.
   */
  private static String filterLauncherForTarget(JavaSemantics semantics, RuleContext ruleContext) {
    // BUILD rule "launcher" attribute
    if (ruleContext.getRule().isAttrDefined("launcher", BuildType.LABEL)
        && ruleContext.attributes().get("launcher", BuildType.LABEL) != null) {
      if (ruleContext.attributes().get("launcher", BuildType.LABEL)
          .equals(JavaSemantics.JDK_LAUNCHER_LABEL)) {
        return null;
      }
      return "launcher";
    }
    // Blaze flag --java_launcher
    JavaConfiguration javaConfig = ruleContext.getFragment(JavaConfiguration.class);
    if (ruleContext.getRule().isAttrDefined(":java_launcher", BuildType.LABEL)
        && ((javaConfig.getJavaLauncherLabel() != null
                && !javaConfig.getJavaLauncherLabel().equals(JavaSemantics.JDK_LAUNCHER_LABEL))
            || semantics.forceUseJavaLauncherTarget(ruleContext))) {
      return ":java_launcher";
    }
    return null;
  }

  /**
   * Javac options require special processing - People use them and expect the
   * options to be tokenized.
   */
  public static List<String> tokenizeJavaOptions(Iterable<String> inOpts) {
    // Ideally, this would be in the options parser. Unfortunately,
    // the options parser can't handle a converter that expands
    // from a value X into a List<X> and allow-multiple at the
    // same time.
    List<String> result = new ArrayList<>();
    for (String current : inOpts) {
      try {
        ShellUtils.tokenize(result, current);
      } catch (ShellUtils.TokenizationException ex) {
        // Tokenization failed; this likely means that the user
        // did not want tokenization to happen on his argument.
        // (Any tokenization where we should produce an error
        // has already been done by the shell that invoked
        // blaze). Therefore, pass the argument through to
        // the tool, so that we can see the original error.
        result.add(current);
      }
    }
    return result;
  }
}
