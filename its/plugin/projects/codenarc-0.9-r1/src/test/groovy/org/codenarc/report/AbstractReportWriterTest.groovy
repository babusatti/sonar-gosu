/*
 * Copyright 2009 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.codenarc.report

import org.codenarc.test.AbstractTestCase
import org.codenarc.results.Results
import org.codenarc.AnalysisContext
import org.codenarc.results.DirectoryResults
import org.codenarc.rule.StubRule
import org.codenarc.ruleset.ListRuleSet

/**
 * Tests for AbstractReportWriter
 *
 * @author Chris Mair
 * @version $Revision: 317 $ - $Date: 2010-04-09 22:39:04 +0400 (Пт, 09 апр 2010) $
 */
class AbstractReportWriterTest extends AbstractTestCase {

    private static final RESULTS = new DirectoryResults()
    private static final ANALYSIS_CONTEXT = new AnalysisContext()
    private static final DEFAULT_STRING = '?'
    private static final CUSTOM_FILENAME = 'abc.txt'
    private static final CONTENTS = 'abc'
    private reportWriter

    void testWriteReport_WritesToDefaultOutputFile_IfOutputFileIsNull() {
        def defaultOutputFile = TestAbstractReportWriter.defaultOutputFile
        reportWriter.writeReport(ANALYSIS_CONTEXT, RESULTS)
        assertOutputFile(defaultOutputFile)
    }

    void testWriteReport_WritesToOutputFile_IfOutputFileIsDefined() {
        reportWriter.outputFile = CUSTOM_FILENAME
        reportWriter.writeReport(ANALYSIS_CONTEXT, RESULTS)
        assertOutputFile(CUSTOM_FILENAME)
    }

    void testWriteReport_WritesToStandardOut_IfWriteToStandardOutIsTrue_String() {
        reportWriter.outputFile = CUSTOM_FILENAME
        reportWriter.writeToStandardOut = "true"
        def output = captureSystemOut {
            reportWriter.writeReport(ANALYSIS_CONTEXT, RESULTS)
        }
        assertFileDoesNotExist(CUSTOM_FILENAME)
        assert output == CONTENTS
    }

    void testWriteReport_WritesToStandardOut_IfWriteToStandardOutIsTrue() {
        reportWriter.outputFile = CUSTOM_FILENAME
        reportWriter.writeToStandardOut = true
        def output = captureSystemOut {
            reportWriter.writeReport(ANALYSIS_CONTEXT, RESULTS)
        }
        assertFileDoesNotExist(CUSTOM_FILENAME)
        assert output == CONTENTS
    }

    void testWriteReport_WritesToStandardOut_AndResetsSystemOut() {
        def originalSystemOut = System.out
        reportWriter.writeToStandardOut = true
        reportWriter.writeReport(ANALYSIS_CONTEXT, RESULTS)
        assert System.out == originalSystemOut
    }

    void testWriteReport_WritesToOutputFile_IfWriteToStandardOutIsNotTrue() {
        reportWriter.outputFile = CUSTOM_FILENAME
        reportWriter.writeToStandardOut = "false"
        reportWriter.writeReport(ANALYSIS_CONTEXT, RESULTS)
        assertOutputFile(CUSTOM_FILENAME)
    }

    void testInitializeResourceBundle_CustomMessagesFileExists() {
        reportWriter.initializeResourceBundle()
        assert reportWriter.getResourceBundleString('htmlReport.titlePrefix', null)   // in "codenarc-base-messages.properties"
        assert reportWriter.getResourceBundleString('abc', null)                      // in "codenarc-messages.properties"
    }

    void testInitializeResourceBundle_CustomMessagesFileDoesNotExist() {
        reportWriter.customMessagesBundleName = 'DoesNotExist'
        reportWriter.initializeResourceBundle()
        assert reportWriter.getResourceBundleString('htmlReport.titlePrefix', null)   // in "codenarc-base-messages.properties"
        assert reportWriter.getResourceBundleString('abc') == DEFAULT_STRING
    }

    void testGetResourceBundleString() {
        reportWriter.initializeResourceBundle()
        assert reportWriter.getResourceBundleString('abc') == '123'
    }

    void testGetResourceBundleString_ReturnsDefaultStringIfKeyNotFound() {
        reportWriter.initializeResourceBundle()
        assert reportWriter.getResourceBundleString('DoesNotExist') == DEFAULT_STRING
    }

    void testGetDescriptionForRule_RuleDescriptionFoundInMessagesFile() {
        reportWriter.initializeResourceBundle()
        def rule = new StubRule(name:'MyRuleXX')
        assert reportWriter.getDescriptionForRule(rule) == 'My Rule XX'
    }

    void testGetDescriptionForRule_DescriptionPropertySetOnRuleObject() {
        reportWriter.initializeResourceBundle()
        def rule = new StubRule(name:'MyRuleXX', description:'xyz')
        assert reportWriter.getDescriptionForRule(rule) == 'xyz'
    }

    void testGetDescriptionForRule_RuleDescriptionNotFoundInMessagesFile() {
        reportWriter.initializeResourceBundle()
        def rule = new StubRule(name:'Unknown')
        assert reportWriter.getDescriptionForRule(rule).startsWith('No description provided')
    }

    void testGetHtmlDescriptionForRule_HtmlRuleDescriptionFoundInMessagesFile() {
        reportWriter.initializeResourceBundle()
        def rule = new StubRule(name:'MyRuleXX')
        assert reportWriter.getHtmlDescriptionForRule(rule) == 'HTML Rule XX'
    }

    void testGetHtmlDescriptionForRule_OnlyRuleDescriptionFoundInMessagesFile() {
        reportWriter.initializeResourceBundle()
        def rule = new StubRule(name:'MyRuleYY')
        assert reportWriter.getHtmlDescriptionForRule(rule) == 'My Rule YY'
    }

    void testGetHtmlDescriptionForRule_DescriptionPropertySetOnRuleObject() {
        reportWriter.initializeResourceBundle()
        def rule = new StubRule(name:'MyRuleXX', description:'xyz')
        assert reportWriter.getHtmlDescriptionForRule(rule) == 'xyz'
    }

    void testGetHtmlDescriptionForRule_NoRuleDescriptionNotFoundInMessagesFile() {
        reportWriter.initializeResourceBundle()
        def rule = new StubRule(name:'Unknown')
        assert reportWriter.getHtmlDescriptionForRule(rule).startsWith('No description provided')
    }

    void testGetFormattedTimestamp() {
        def timestamp = new Date(1262361072497)
        reportWriter.getTimestamp = { timestamp }
        def expected = java.text.DateFormat.getDateTimeInstance().format(timestamp)
        assert reportWriter.getFormattedTimestamp() == expected
    }

    void testGetSortedRules() {
        def ruleSet = new ListRuleSet([new StubRule(name:'BB'), new StubRule(name:'AA'), new StubRule(name:'DD'), new StubRule(name:'CC')])
        def analysisContext = new AnalysisContext(ruleSet:ruleSet)
        def sorted = reportWriter.getSortedRules(analysisContext)
        log(sorted)
        assert sorted.name == ['AA', 'BB', 'CC', 'DD']
    }

    void testGetSortedRules_RemovesDisabledRules() {
        def ruleSet = new ListRuleSet([new StubRule(name:'BB', enabled:false), new StubRule(name:'AA'), new StubRule(name:'DD'), new StubRule(name:'CC', enabled:false)])
        def analysisContext = new AnalysisContext(ruleSet:ruleSet)
        def sorted = reportWriter.getSortedRules(analysisContext)
        log(sorted)
        assert sorted.name == ['AA', 'DD']
    }

    void testGetCodeNarcVersion() {
        assert reportWriter.getCodeNarcVersion() == new File('src/main/resources/codenarc-version.txt').text
    }

    void setUp() {
        super.setUp()
        reportWriter = new TestAbstractReportWriter()
    }

    private void assertOutputFile(String outputFile) {
        def file = new File(outputFile)
        assert file.exists(), "The output file [$outputFile] does not exist"
        def contents = file.text
        file.delete()
        assert contents == CONTENTS
    }

    private void assertFileDoesNotExist(String filename) {
        assert new File(filename).exists() == false
    }

}

/**
 * Concrete subclass of AbstractReportWriter for testing
 */
class TestAbstractReportWriter extends AbstractReportWriter {
    static defaultOutputFile = 'TestReportWriter.txt'
    String title 

    void writeReport(Writer writer, AnalysisContext analysisContext, Results results) {
        writer.write('abc')
        writer.flush()
    }
}