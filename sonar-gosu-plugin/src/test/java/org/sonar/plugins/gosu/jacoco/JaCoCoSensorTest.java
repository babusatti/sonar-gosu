/*
 * Sonar Gosu Plugin
 * Copyright (C) 2016-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.gosu.jacoco;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.gosu.foundation.Gosu;
import org.sonar.test.TestUtils;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JaCoCoSensorTest {

  private File jacocoExecutionData;
  private DefaultInputFile inputFile;
  private JaCoCoConfiguration configuration;
  private PathResolver pathResolver;
  private JaCoCoSensor sensor;

  @Before
  public void setUp() throws Exception {
    this.jacocoExecutionData = initWithJaCoCoVersion("JaCoCoSensor_0_7_4");
  }

  private File initWithJaCoCoVersion(String jacocoVersion) throws IOException {
    File outputDir = TestUtils.getResource("/org/sonar/plugins/gosu/jacoco/" + jacocoVersion + "/");
    File jacocoExecutionData = new File(outputDir, "jacoco-ut.exec");

    Files.copy(TestUtils.getResource("/org/sonar/plugins/gosu/jacoco/Hello.class.toCopy"),
      new File(jacocoExecutionData.getParentFile(), "Hello.class"));
    Files.copy(TestUtils.getResource("/org/sonar/plugins/gosu/jacoco/Hello$InnerClass.class.toCopy"),
      new File(jacocoExecutionData.getParentFile(), "Hello$InnerClass.class"));

    Gosu gosu = mock(Gosu.class);
    when(gosu.getBinaryDirectories()).thenReturn(Lists.newArrayList("."));

    configuration = mock(JaCoCoConfiguration.class);
    when(configuration.shouldExecuteOnProject(true)).thenReturn(true);
    when(configuration.shouldExecuteOnProject(false)).thenReturn(false);
    when(configuration.getReportPath()).thenReturn(jacocoExecutionData.getPath());

    DefaultFileSystem fileSystem = new DefaultFileSystem(jacocoExecutionData.getParentFile());
    inputFile = new DefaultInputFile("", "example/Hello.groovy")
      .setLanguage(Gosu.KEY)
      .setType(Type.MAIN);
    inputFile.setLines(50);
    fileSystem.add(inputFile);

    pathResolver = mock(PathResolver.class);
    sensor = new JaCoCoSensor(gosu, configuration, fileSystem, pathResolver);

    return jacocoExecutionData;
  }

  @Test
  public void testSensorDefinition() {
    assertThat(sensor.toString()).isEqualTo("Gosu JaCoCoSensor");
  }

  @Test
  public void test_description() {
    DefaultSensorDescriptor defaultSensorDescriptor = new DefaultSensorDescriptor();
    sensor.describe(defaultSensorDescriptor);
    assertThat(defaultSensorDescriptor.languages()).containsOnly(Gosu.KEY);
  }

  @Test
  public void should_Execute_On_Project_only_if_exec_exists() {
    when(configuration.getReportPath()).thenReturn("ut.exec");

    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(jacocoExecutionData);
    assertThat(sensor.shouldExecuteOnProject()).isTrue();

    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(jacocoExecutionData.getParentFile());
    assertThat(sensor.shouldExecuteOnProject()).isFalse();

    File outputDir = TestUtils.getResource(JaCoCoSensorTest.class, ".");
    File fakeExecFile = new File(outputDir, "ut.not.found.exec");
    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(fakeExecFile);
    assertThat(sensor.shouldExecuteOnProject()).isFalse();

    when(pathResolver.relativeFile(any(File.class), eq("ut.exec"))).thenReturn(fakeExecFile);
    when(configuration.shouldExecuteOnProject(false)).thenReturn(true);
    assertThat(sensor.shouldExecuteOnProject()).isTrue();
  }

  @Test
  public void test_read_execution_data_with_jacoco_0_7_4() {
    when(pathResolver.relativeFile(any(File.class), argThat(Matchers.endsWith(".exec")))).thenReturn(jacocoExecutionData);

    SensorContextTester context = SensorContextTester.create(new File(""));
    sensor.execute(context);

    verifyMeasures(context);
  }

  @Test
  public void test_read_execution_data_with_jacoco_0_7_5() throws IOException {
    File jacocoExecutionData = initWithJaCoCoVersion("JaCoCoSensor_0_7_5");
    when(pathResolver.relativeFile(any(File.class), argThat(Matchers.endsWith(".exec")))).thenReturn(jacocoExecutionData);

    SensorContextTester context = SensorContextTester.create(new File(""));
    sensor.execute(context);

    verifyMeasures(context);
  }

  private void verifyMeasures(SensorContextTester context) {
    int[] oneHitlines = {9, 10, 14, 15, 17, 21, 29, 32, 33, 42, 47};
    int[] zeroHitlines = {25, 30, 38};
    int[] conditionLines = {14, 29, 30};
    int[] coveredConditions = {2, 1, 0};

    for (int zeroHitline : zeroHitlines) {
      assertThat(context.lineHits(":example/Hello.groovy", CoverageType.UNIT, zeroHitline)).isEqualTo(0);
    }
    for (int oneHitline : oneHitlines) {
      assertThat(context.lineHits(":example/Hello.groovy", CoverageType.UNIT, oneHitline)).isEqualTo(1);
    }
    for (int i = 0; i < conditionLines.length; i++) {
      int conditionLine = conditionLines[i];
      assertThat(context.conditions(":example/Hello.groovy", CoverageType.UNIT, conditionLine)).isEqualTo(2);
      assertThat(context.coveredConditions(":example/Hello.groovy", CoverageType.UNIT, conditionLine)).isEqualTo(coveredConditions[i]);
    }
  }
}
